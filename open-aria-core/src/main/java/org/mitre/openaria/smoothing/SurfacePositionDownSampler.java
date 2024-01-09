
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.min;
import static org.mitre.caasd.commons.Spherical.feetPerNM;

import java.time.Duration;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.OptionalDouble;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointField;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Spherical;

/**
 * A SurfacePositionDownSampler will "thin out" a Track that contains nearly-duplicate Point data
 * because the aircraft is stationary. This data cleaner is similar to {@link DistanceDownSampler}
 * in that it removes clusters of points that have similar locations leaving behind a single
 * representative point.  In contrast to {@link DistanceDownSampler}, this data cleaner changes the
 * values of that representative point to have the average speed and average latlong of the cluster.
 * This feature is important for the smoothing of surface tracks because it ensures stationary
 * points don't incorrectly inherit large speeds from nearby points.
 */
public class SurfacePositionDownSampler implements DataCleaner<MutableTrack> {

    private final double clusterRadiusInNm;
    private final Duration heartBeatPeriod;

    /**
     * Create a SurfacePositionDownSampler that ensures no two sequential points (in the output
     * track) will be within 30 feet of each other unless those two points are also separated by at
     * least 5 seconds or more.
     */
    public SurfacePositionDownSampler() {
        this(15.0 / feetPerNM(), Duration.ofSeconds(5));
    }

    /**
     * Create a SurfacePositionDownSampler that detects clusters of points within a specified radius
     * and specified time interval.  All points in the cluster are removed from the track except for
     * the first point.  This first point is updated so that its latlong and speed are the average
     * latlong and average speed of the cluster.
     *
     * @param clusterRadiusInNm The radius of a circle inside which all points must lie order to be
     *                          considered a cluster.
     * @param heartBeatPeriod   The time interval after which a new cluster must be defined.  This
     *                          ensures the resulting track has at least one point in every time
     *                          period of this length.
     */
    SurfacePositionDownSampler(double clusterRadiusInNm, Duration heartBeatPeriod) {
        this.clusterRadiusInNm = clusterRadiusInNm;
        this.heartBeatPeriod = checkNotNull(heartBeatPeriod);
    }

    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        NavigableSet<MutablePoint> points = track.points();
        MutablePoint endPoint = points.last();
        ArrayList<MutablePoint> pointsToRemove = new ArrayList<>();

        MutablePoint firstPoint = null;
        LatLong avgLatLong = null;
        Integer numPoints = null;

        for (MutablePoint point : points) {
            if (firstPoint == null) {
                firstPoint = point;
                avgLatLong = point.latLong();
                numPoints = 1;
                continue;
            }

            if (withinClusterRadius(point, avgLatLong, numPoints) && withinClusterTime(point, firstPoint)) {
                avgLatLong = updateAvgLatLong(avgLatLong, point.latLong(), numPoints);
                numPoints += 1;
                if (!point.equals(endPoint)) {
                    continue;
                }
            }

            if (numPoints > 1) {
                Double avgClusterSpeedInKnots = averageClusterSpeed(points, firstPoint, point);
                firstPoint.set(PointField.LAT_LONG, avgLatLong);
                firstPoint.set(PointField.SPEED, avgClusterSpeedInKnots);
                pointsToRemove.addAll(points.subSet(firstPoint, false, point, false));
            }

            firstPoint = point;
            avgLatLong = point.latLong();
            numPoints = 1;
        }

        points.removeAll(pointsToRemove);
        return points.isEmpty() ? Optional.empty() : Optional.of(track);
    }

    private boolean withinClusterTime(MutablePoint point, Point firstPoint) {

        return point.time().isBefore(firstPoint.time().plus(heartBeatPeriod));
    }

    private boolean withinClusterRadius(MutablePoint point, LatLong avgLatLong, int numPoints) {

        LatLong newAvgLatLong = updateAvgLatLong(avgLatLong, point.latLong(), numPoints);
        return Spherical.distanceInNM(point.latLong(), newAvgLatLong) < clusterRadiusInNm;
    }

    private LatLong updateAvgLatLong(LatLong avgLatLong, LatLong newLatLong, int numPoints) {

        double updatedLat = (avgLatLong.latitude() * numPoints + newLatLong.latitude()) / (numPoints + 1);
        double updatedLon = (avgLatLong.longitude() * numPoints + newLatLong.longitude()) / (numPoints + 1);

        return LatLong.of(updatedLat, updatedLon);
    }

    private Double averageClusterSpeed(NavigableSet<MutablePoint> points, MutablePoint firstPoint, MutablePoint lastPoint) {

        Duration clusterDuration = Duration.between(firstPoint.time(), lastPoint.time());

        OptionalDouble avgSpeedInKnots = points.subSet(firstPoint, true, lastPoint, false).stream()
            .mapToDouble(Point::speedInKnots)
            .average();

        double maxSpeedInKnots = 2 * clusterRadiusInNm / (clusterDuration.toMillis() / 3600000.0);

        return avgSpeedInKnots.isPresent()
            ? min(avgSpeedInKnots.getAsDouble(), maxSpeedInKnots)
            : maxSpeedInKnots;
    }
}