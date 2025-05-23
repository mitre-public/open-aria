package org.mitre.openaria.smoothing;

import static java.lang.Double.max;
import static java.util.stream.Collectors.toList;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.KineticPosition;
import org.mitre.caasd.commons.KineticRecord;
import org.mitre.caasd.commons.Position;
import org.mitre.caasd.commons.PositionRecord;
import org.mitre.caasd.commons.math.locationfit.LocalPolyInterpolator;
import org.mitre.caasd.commons.math.locationfit.PositionInterpolator;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

/**
 * This class is a replacement for AlongTrackFilter and CrossTrackFilter.
 *
 * <p>The replacement was necessary to remove the JBLAS dependency under-the-hood of
 * AlongTrackFilter and CrossTrackFilter
 */
public class TrackFilter<T> implements DataCleaner<Track<T>> {

    /* This fitter doubles its Window Size when the "narrower window" does not contain enough sample data. */
    private final PositionInterpolator fitter;

    public TrackFilter(Duration timeWindow) {
        this.fitter = new TwoStageInterpolator(timeWindow);
    }

    public TrackFilter() {
        this(Duration.ofMinutes(1));
    }

    /**
     * Set the ALONG_TRACK_DISTANCE, SPEED, LAT_LONG, COURSE_IN_DEGREES, and CURVATURE of a track
     *
     * @param track A Track of mutable points
     *
     * @return An empty Optional when the filtering process cannot produce an output track, OR the filtered track
     */
    @Override
    public Optional<Track<T>> clean(Track<T> track) {

        TreeSet<Point<T>> points = new TreeSet<>(track.points());

        if (points.size() == 1) {
            Point<T> pt = points.first();
            //if points supported this field here is where we'd add it
            Point<T> corrected = Point.builder(pt).speedInKnots(0.0).build();
            return Optional.of(Track.of(List.of(corrected)));
        }

        //Extract the potentially noisy physical position of each input point
        List<PositionRecord<Point<T>>> originalPositions = points.stream()
            .map(pt -> asPositionRecord(pt))
            .collect(toList());

        //Deduce noise-reduced Lat/Long/Speed/Course/etc values
        List<KineticRecord<Point<T>>> fitPositions = points.stream()
            .map(pt -> fitter.floorInterpolate(originalPositions, pt.time()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();


        //Rebuild the "points" NavigableSet from only the valid data
        List<Point<T>> smoothedPoints = fitPositions.stream()
            .map(kr -> makeAdjustedPoint(kr))
            .collect(toList());

        return smoothedPoints.isEmpty()
            ? Optional.empty()
            : Optional.of(Track.of(smoothedPoints));
    }

    private Point<T> makeAdjustedPoint(KineticRecord<Point<T>> kr) {

        /*
         * There are a few cases instances when a computed speed will be negative. This can
         * occur when the speed should be zero but there is a small amount of numeric error
         * making the speed just barely negative. It can also occur when the aircraft is
         * accelerating or decelerating extremely quick and the first (takeoff) or last
         * (landing) point is "over fit" into negative territory.
         */
        return Point.builder(kr.datum())
            .speedInKnots(max(0.0, kr.kinetics().speed().inKnots()))
            .latLong(kr.kinetics().latLong())
            .courseInDegrees(kr.kinetics().course().inDegrees())
            .build();
    }

    private PositionRecord<Point<T>> asPositionRecord(Point<T> point) {
        Position p = new Position(point.time(), point.latLong());
        return new PositionRecord<>(point, p);
    }

    /**
     * A TwoStageInterpolator uses two LocalPolyInterpolators. The first stage is for normal
     * operations when enough data is in the sampling window to support creating polynomial fits.
     * The second stage is for abnormal operations when the desired sampling window does not contain
     * enough sample data to enable polynomial fitting.  In this case, the sampling window is
     * doubled in size in the hopes of finding additional data that will permit returning an answer
     * (as opposed to returning an empty optional.)
     */
    private static class TwoStageInterpolator implements PositionInterpolator {

        //The core PositionInterpolator has a narrow time aperture
        PositionInterpolator core;

        //The backup PositionInterpolator has a time aperture twice as wide
        PositionInterpolator backup;

        TwoStageInterpolator(Duration windowSize) {
            this.core = new LocalPolyInterpolator(windowSize, 3, true);
            this.backup = new LocalPolyInterpolator(windowSize.multipliedBy(2L), 3, true);
        }

        @Override
        public Optional<KineticPosition> interpolate(List<Position> positionData, Instant sampleTime) {

            Optional<KineticPosition> firstTry = core.interpolate(positionData, sampleTime);

            return firstTry.isPresent()
                ? firstTry
                : backup.interpolate(positionData, sampleTime);
        }
    }
}
