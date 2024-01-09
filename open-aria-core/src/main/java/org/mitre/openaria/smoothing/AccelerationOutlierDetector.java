
package org.mitre.openaria.smoothing;

import static java.lang.Math.abs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.math.Vector;

public class AccelerationOutlierDetector implements DataCleaner<MutableTrack> {
    private static final double ACCELERATION_THRESHOLD = 70.0;
    private static final Duration OUTLIER_INSPECTION_WINDOW = Duration.ofSeconds(15);
    private static final Duration DATA_GAP_THRESHOLD = Duration.ofSeconds(10);

    /**
     * Create a cleaned version of the input track that has high acceleration points caused by data
     * anomalies removed.  The filter only checks for acceleration outliers occuring within 15 of
     * data outages lasting more than 10 seconds.  The ASDE-X data is most likely to contain errant
     * points around these data gaps.  This DataCleaner should be applied before {@literal
     * FillMissingSpeeds} to avoid missing speeds being incorrectly estimated.
     *
     * @param inputTrack A Track
     *
     * @return An Optional Track with high acceleration points removed.
     */
    @Override
    public Optional<MutableTrack> clean(MutableTrack inputTrack) {

        Collection<MutablePoint> outliers = getOutliers(inputTrack);

        inputTrack.points().removeAll(outliers);

        return (inputTrack.points().isEmpty())
            ? Optional.empty()
            : Optional.of(inputTrack);
    }

    /**
     * Find the points in the input track with high approximate accelerations. Approximate
     * accelerations are computed based on the location of three consecutive points. A large
     * acceleration is considered greater than 70 knots per second.  Return all points between a
     * data outage and a nearby high acceleration point.  This function will only return points if
     * they occur within 15 seconds of a data outage. The first and last 15 seconds of a track are
     * also analyzed.
     *
     * @param track A Track
     *
     * @return The set of Points which occur between a data outage and a nearby time of high
     *     acceleration.
     */
    private NavigableSet<MutablePoint> getOutliers(MutableTrack track) {

        TreeSet<MutablePoint> outliers = new TreeSet<>();

        RectangularMapProjection projectionInNm = new RectangularMapProjection(track.points().first());

        Collection<DataGap> dataGaps = dataGaps(track);

        for (DataGap dataGap : dataGaps) {
            outliers.addAll(outliersPrecedingDataGap(dataGap.pointsPrecedingDataGap, projectionInNm));
            outliers.addAll(outliersFollowingDataGap(dataGap.pointsFollowingDataGap, projectionInNm));
        }

        return outliers;
    }

    private Collection<DataGap> dataGaps(MutableTrack track) {

        ArrayList<DataGap> dataGaps = new ArrayList<>();

        dataGaps.add(DataGap.withOnlyFollowingPoints(trackHead(track)));
        dataGaps.add(DataGap.withOnlyPrecedingPoints(trackTail(track)));

        MutablePoint previousPoint = track.points().first();
        for (MutablePoint point : track.points()) {

            if (isTimeGap(previousPoint, point)) {
                dataGaps.add(DataGap.withBookends(
                    pointsPreceding(track, previousPoint),
                    pointsFollowing(track, point)
                ));
            }
            previousPoint = point;
        }

        return dataGaps;
    }

    private NavigableSet<MutablePoint> pointsPreceding(MutableTrack track, MutablePoint endPoint) {

        MutablePoint startPoint = Point.builder()
            .time(endPoint.time().minus(OUTLIER_INSPECTION_WINDOW))
            .buildMutable();

        return track.points().subSet(startPoint, false, endPoint, true);
    }

    private NavigableSet<MutablePoint> pointsFollowing(MutableTrack track, MutablePoint startPoint) {

        MutablePoint endPoint = Point.builder()
            .time(startPoint.time().plus(OUTLIER_INSPECTION_WINDOW))
            .buildMutable();

        return track.points().subSet(startPoint, true, endPoint, false);
    }

    private NavigableSet<MutablePoint> trackHead(MutableTrack track) {

        return pointsFollowing(track, track.points().first());
    }

    private NavigableSet<MutablePoint> trackTail(MutableTrack track) {

        return pointsPreceding(track, track.points().last());
    }

    private NavigableSet<MutablePoint> outliersPrecedingDataGap(NavigableSet<MutablePoint> pointsPrecedingDataGap, RectangularMapProjection projectionInNm) {

        NavigableSet<MutablePoint> outliers = new TreeSet<>();

        MutablePoint point1 = null;
        MutablePoint point2 = null;
        MutablePoint point3 = null;

        for (MutablePoint point : pointsPrecedingDataGap) {
            point1 = point2;
            point2 = point3;
            point3 = point;

            if (point1 == null || point2 == null) {
                continue;
            }

            if (accelerationInKtPerSec(point1, point2, point3, projectionInNm) > ACCELERATION_THRESHOLD) {
                outliers.addAll(pointsPrecedingDataGap.tailSet(point3, true));
                break;
            }
        }

        return outliers;
    }

    private NavigableSet<MutablePoint> outliersFollowingDataGap(NavigableSet<MutablePoint> pointsFollowingDataGap, RectangularMapProjection projectionInNm) {

        NavigableSet<MutablePoint> outliers = new TreeSet<>();

        MutablePoint point1 = null;
        MutablePoint point2 = null;
        MutablePoint point3 = null;

        for (MutablePoint point : pointsFollowingDataGap) {
            point1 = point2;
            point2 = point3;
            point3 = point;

            if (point1 == null || point2 == null) {
                continue;
            }

            if (accelerationInKtPerSec(point1, point2, point3, projectionInNm) > ACCELERATION_THRESHOLD) {
                outliers.addAll(pointsFollowingDataGap.headSet(point1, true));
            }
        }

        return outliers;
    }

    private boolean isTimeGap(Point firstPoint, Point secondPoint) {

        return secondPoint.time().isAfter(firstPoint.time().plus(DATA_GAP_THRESHOLD));
    }

    private double accelerationInKtPerSec(Point point1, Point point2, Point point3, RectangularMapProjection projectionInNm) {
        double magnitudeOfVelocityChange = magnitudeOfVelocityChangeInKnots(point1, point2, point3, projectionInNm);
        double timeChange = avgTimeChangeInSec(point1, point3);

        return abs(magnitudeOfVelocityChange / timeChange);
    }

    private Double magnitudeOfVelocityChangeInKnots(Point point1, Point point2, Point point3, RectangularMapProjection projectionInNm) {

        Vector pointProjection1 = projectionInNm.coordinateVector(point1);
        Vector pointProjection2 = projectionInNm.coordinateVector(point2);
        Vector pointProjection3 = projectionInNm.coordinateVector(point3);

        Double deltaHours1 = Duration.between(point1.time(), point2.time()).toMillis() / 3600000.0;
        Double deltaHours2 = Duration.between(point2.time(), point3.time()).toMillis() / 3600000.0;

        Vector velocity1 = pointProjection2.minus(pointProjection1).times(1 / deltaHours1);
        Vector velocity2 = pointProjection3.minus(pointProjection2).times(1 / deltaHours2);

        return velocity1.minus(velocity2).magnitude();
    }

    private Double avgTimeChangeInSec(Point firstPoint, Point lastPoint) {
        return Duration.between(firstPoint.time(), lastPoint.time()).toMillis() / (2000.0);
    }

    static class DataGap {

        private final NavigableSet<MutablePoint> pointsPrecedingDataGap;
        private final NavigableSet<MutablePoint> pointsFollowingDataGap;

        private DataGap(NavigableSet<MutablePoint> pointsPrecedingDataGap, NavigableSet<MutablePoint> pointsFollowingDataGap) {

            this.pointsPrecedingDataGap = pointsPrecedingDataGap;
            this.pointsFollowingDataGap = pointsFollowingDataGap;
        }

        static DataGap withBookends(NavigableSet<MutablePoint> pointsPrecedingDataGap, NavigableSet<MutablePoint> pointsFollowingDataGap) {

            return new DataGap(pointsPrecedingDataGap, pointsFollowingDataGap);
        }

        static DataGap withOnlyPrecedingPoints(NavigableSet<MutablePoint> pointsPrecedingDataGap) {

            return new DataGap(pointsPrecedingDataGap, new TreeSet<>());
        }

        static DataGap withOnlyFollowingPoints(NavigableSet<MutablePoint> pointsFollowingDataGap) {

            return new DataGap(new TreeSet<>(), pointsFollowingDataGap);
        }
    }
}