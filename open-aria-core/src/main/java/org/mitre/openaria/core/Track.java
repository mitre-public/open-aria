package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableNavigableSet;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.mitre.openaria.core.utils.Misc.mostCommon;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.out.JsonWritable;
import org.mitre.openaria.core.temp.Extras.HasAircraftDetails;

/**
 * A Track is a NavigableSet of Points backed by a single type of location data T.
 * <p>
 * Track provides access to the underlying point data as well as a handful of convenience methods
 * that operate on the internal Point data. Ideally, all Tracks would contain an immutable
 * Collection of Points. This is not strictly required, but the convenience constructors always
 * migrate Point data to an unmodifiableNavigableSet
 * <p>
 * @param <T> The format of the underlying Point data
 */
public record Track<T>(NavigableSet<Point<T>> points) implements JsonWritable {

    /** Ensure a Track uses a non-null, non-empty Collection of Points. */
    public Track {
        requireNonNull(points);
        checkArgument(!points.isEmpty());
    }

    public static <T> Track<T> of(Collection<Point<T>> points) {
        TreeSet<Point<T>> pts = new TreeSet<>(points);
        return new Track<>(unmodifiableNavigableSet(pts));
    }

    /** @return The Points inside this track (sorted in time order). */
    public NavigableSet<Point<T>> points() {
        return this.points;
    }

    /** @return The LatLong's of the points inside this track. The returned list is mutable */
    public List<LatLong> pointLatLongs() {
        // Intentionally choosing to return a mutable list
        return points().stream().map(p -> p.latLong()).collect(toCollection(ArrayList::new));
    }

    public int size() {
        return points().size();
    }

    public String trackId() {
        Collection<String> trackIds = points().stream().map(p -> p.trackId()).toList();

        return mostCommon(trackIds);
    }

    public String aircraftType() {

        List<String> aircraftTypes = points().stream()
            .filter(p -> p.rawData() instanceof HasAircraftDetails)
            .map(p -> ((HasAircraftDetails) p.rawData()).acDetails().aircraftType())
            .filter(acType -> nonNull(acType))
            .toList();

        return aircraftTypes.isEmpty() ? "UNKNOWN" : mostCommon(aircraftTypes);
    }

    public String callsign() {
        List<String> callsigns = points().stream()
            .filter(p -> p.rawData() instanceof HasAircraftDetails)
            .map(p -> ((HasAircraftDetails) p.rawData()).acDetails().callsign())
            .filter(callsign -> nonNull(callsign))
            .toList();

        return callsigns.isEmpty() ? "UNKNOWN" : mostCommon(callsigns);
    }

    /**
     * @return The minimum TimeWindow that contains all Points within this Track.
     */
    public TimeWindow asTimeWindow() {
        return TimeWindow.of(
            points().first().time(),
            points().last().time()
        );
    }

    /**
     * @param otherTrack A different Track
     *
     * @return An Optional containing the overlap (in time) between these two Tracks.
     */
    public Optional<TimeWindow> getOverlapWith(Track<T> otherTrack) {
        return asTimeWindow().getOverlapWith(otherTrack.asTimeWindow());
    }

    /**
     * @param time The instant in time to create a Point for.
     *
     * @return An interpolated Point that reflects the Points immediately before and after the
     *     supplied moment in time. An empty optional is returned if the time parameter is outside
     *     this track's TimeWindow (this method interpolates, it does not extrapolate).
     */
    public Optional<Point<T>> interpolatedPoint(Instant time) {

        //here we interpolate, we do not extrapolate.
        if (!asTimeWindow().contains(time)) {
            return Optional.empty();
        }

        Point<T> stub = points.first();
        Point<T> pointWithTime = Point.builder(stub).time(time).latLong(0.0, 0.0).build();

        Point<T> ceiling = points.ceiling(pointWithTime);
        Point<T> floor = points.floor(pointWithTime);

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
    public NavigableSet<Point<T>> kNearestPoints(Instant time, int k) {
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
    public Point<T> nearestPoint(Instant time) {
        return kNearestPoints(time, 1).first();
    }

    /**
     * Create a distinct collection of a Points that match this predicate. This is shorthand for:
     * points().stream().filter(predicate).collect(toCollection(TreeSet::new));
     * <p>
     * This method exhaustively searches the track. This method DOES NOT take advantage of time
     * ordering to speed up time-based searches. Use
     * {@link Track#subset(org.mitre.caasd.commons.TimeWindow)} and
     * {@link Track#subset(java.time.Instant, java.time.Instant) } to take advantage of this
     * optimization.
     *
     * @param includeIfTrue A predicate
     *
     * @return A completely distinct collection of Points.
     */
    public TreeSet<Point<T>> subset(Predicate<Point<T>> includeIfTrue) {
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
     * @param window The sampling window
     *
     * @return A completely distinct collection of Points.
     */
    public TreeSet<Point<T>> subset(TimeWindow window) {
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
     * @param startTime The start of the sampling window
     * @param endTime The end of the sampling window
     *
     * @return A completely distinct collection of Points.
     */
    public TreeSet<Point<T>> subset(Instant startTime, Instant endTime) {
        return Points.subset(
            TimeWindow.of(startTime, endTime),
            points()
        );
    }

    public Instant startTime() {
        return points().first().time();
    }

    public Instant endTime() {
        return points().last().time();
    }
}
