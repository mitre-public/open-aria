

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static org.mitre.openaria.core.PointField.AIRCRAFT_TYPE;
import static org.mitre.openaria.core.PointField.CALLSIGN;
import static org.mitre.openaria.core.PointField.TRACK_ID;
import static org.mitre.openaria.core.Points.mostCommon;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.out.JsonWritable;

/**
 * A Track is just a collection of radar sensor hits.
 * <p>
 * The Track interface provides access to the underlying point data as well as a handful of
 * convenience methods that operate on that data. Ideally, all Tracks would be immutable. However,
 * this interface does not require immutability because it is far more efficient to directly
 * manipulate Track data when implementing DataCleaners that correct flawed data (like outlier
 * removers and other noise reduction techniques.)
 */
public interface Track extends JsonWritable {

    /** @return The Points that are inside this track (sorted in time order). */
    NavigableSet<? extends Point> points();

    public default int size() {
        return points().size();
    }

    public default String trackId() {
        return mostCommon(TRACK_ID, points());
    }

    public default String aircraftType() {
        String aircraftType = mostCommon(AIRCRAFT_TYPE, points());
        return (nonNull(aircraftType))
            ? aircraftType
            : "UNKNOWN";
    }

    public default String callsign() {
        String callsign = mostCommon(CALLSIGN, points());
        return (nonNull(callsign))
            ? callsign
            : "UNKNOWN";
    }

    /**
     * @return A String that represent this Track as if it were a sequence of raw NOP Radar Hit (RH)
     *     Messages. This method relies on each contained Point object to provide the best possible
     *     "nop representation" of itself.
     */
    public default String asNop() {

        //rely on the each point to generate the best possible "nop representation" of itself.
        StringBuilder sb = new StringBuilder();
        for (Point point : points()) {
            sb.append(point.asNop()).append("\n");
        }
        return sb.toString();
    }

    /**
     * @return The minimum TimeWindow that contains all Points within this Track."
     */
    public default TimeWindow asTimeWindow() {
        NavigableSet<? extends Point> pts = points();
        return TimeWindow.of(
            pts.first().time(),
            pts.last().time()
        );
    }

    /**
     * @param otherTrack A different Track
     *
     * @return An Optional containing the overlap (in time) between these two Tracks.
     */
    public default Optional<TimeWindow> getOverlapWith(Track otherTrack) {
        return asTimeWindow().getOverlapWith(otherTrack.asTimeWindow());
    }

    /**
     * @param time The instant in time to create a Point for.
     *
     * @return An interpolated Point that reflects the Points immediately before and after the
     *     supplied moment in time. An empty optional is returned if the time parameter is outside
     *     this track's TimeWindow (this method interpolates, it does not extrapolate).
     */
    public default Optional<Point> interpolatedPoint(Instant time) {

        //here we interpolate, we do not extrapolate.
        if (!asTimeWindow().contains(time)) {
            return Optional.empty();
        }

        Point pointWithTime = Point.builder().time(time).build();

        NavigableSet<Point> points = (NavigableSet<Point>) points();
        Point ceiling = points.ceiling(pointWithTime);
        Point floor = points.floor(pointWithTime);

        if (floor == null) {
            //the floor is null when "time" parameter is the exact time of the 1st point
            floor = ceiling;
        }

        return Optional.of(Interpolate.interpolate(floor, ceiling, time));
    }

    /**
     * Find the k Points in this Track with time values closest to the given input time.
     *
     * @param time The time "anchor" for the kNN computation
     * @param k    The maximum number of Points that should be retrieved.
     *
     * @return A NavigableSet contain at most k Points from this Track.
     */
    public default NavigableSet<Point> kNearestPoints(Instant time, int k) {
        return Points.fastKNearestPoints(points(), time, k);
    }

    /**
     * Find the Point in this Track whose time value is closest to the given input time. This method
     * is equivalent to <code> kNearestPoints(time, 1).first(); </code>
     *
     * @param time The time "anchor" for the kNN computation
     *
     * @return Exactly 1 Point from this Track
     */
    public default Point nearestPoint(Instant time) {
        return kNearestPoints(time, 1).first();
    }

    /**
     * @return A completely independent, and mutable, copy of this Track
     */
    public default MutableTrack mutableCopy() {
        ArrayList<MutablePoint> mutablePoints = points().stream()
            .map(p -> EphemeralPoint.from(p))
            .collect(toCollection(ArrayList::new));

        return MutableTrack.of(mutablePoints);
    }

    /**
     * Create a distinct collection of a Points that match this predicate. This is shorthand for:
     * points().stream().filter(predicate).collect(toCollection(TreeSet::new));
     * <p>
     * This method exhaustively searches the track. This method DOES NOT take advantage of time
     * ordering to speed up time-based searches. Use {@link Track#subset(org.mitre.caasd.commons.TimeWindow)}
     * and {@link Track#subset(java.time.Instant, java.time.Instant) } to take advantage of this
     * optimization.
     *
     * @param includeIfTrue A predicate
     *
     * @return A completely distinct collection of Points.
     */
    public default NavigableSet<? extends Point> subset(Predicate<Point> includeIfTrue) {
        checkNotNull(includeIfTrue);

        return points()
            .stream()
            .filter(includeIfTrue)
            .collect(toCollection(TreeSet::new));
    }

    /**
     * Create a distinct collection of a Points that fall within this time window (inclusive)
     * <p>
     * This method D0ES NOT exhaustively search the track for points that occur within this window.
     * This method is optimized to take advantage of time ordering of the points to speed up this
     * time-based query.
     *
     * @param window
     *
     * @return A completely distinct collection of Points.
     */
    public default NavigableSet<? extends Point> subset(TimeWindow window) {
        checkNotNull(window);
        return subset(window.start(), window.end());
    }

    /**
     * Create a distinct collection of a Points that fall within this time window (inclusive)
     * <p>
     * This method D0ES NOT exhaustively search the track for points that occur within this window.
     * This method is optimized to take advantage of time ordering of the points to speed up this
     * time-based query.
     *
     * @param startTime
     * @param endTime
     *
     * @return A completely distinct collection of Points.
     */
    public default NavigableSet<? extends Point> subset(Instant startTime, Instant endTime) {
        return Points.subset(
            TimeWindow.of(startTime, endTime),
            (NavigableSet<Point>) points()
        );
    }

    public default Instant startTime() {
        return points().first().time();
    }

    public default Instant endTime() {
        return points().last().time();
    }
}
