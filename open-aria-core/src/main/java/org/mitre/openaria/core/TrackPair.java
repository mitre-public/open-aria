
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.SeparationTimeSeries.dynamicTimeStep;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.TimeWindow;

/**
 * TrackPair contains exactly two non-null Tracks. This class provides two main benefits. It helps
 * simplify the signatures of methods and classes that operate on Track Pairs. This class also
 * provides a location for common TrackPair operations like "maxDistanceBetween" and "timeOverlap".
 */
public class TrackPair implements Serializable {

    private final Track track1;

    private final Track track2;

    private transient SeparationTimeSeries sepInfo;

    public TrackPair(Track track1, Track track2) {
        this.track1 = checkNotNull(track1);
        this.track2 = checkNotNull(track2);
    }

    public static TrackPair of(Track track1, Track track2) {
        return new TrackPair(track1, track2);
    }

    /**
     * Create a TrackPair from a Collection that contains exactly two tracks
     *
     * @param tracks A Collection containing exactly two tracks
     *
     * @return The requested pair
     */
    public static <T> TrackPair from(Collection<Track<T>> tracks) {
        checkNotNull(tracks);
        checkArgument(tracks.size() == 2, "Input collection must contain exactly two tracks, size = " + tracks.size());
        Track[] array = tracks.toArray(new Track[2]);
        return new TrackPair(array[0], array[1]);
    }

    public Track track1() {
        return track1;
    }

    public Track track2() {
        return track2;
    }

    /**
     * @return A SeparationTimeSeries object that describes (A) the separation between the two
     *     aircraft and (B) how that separation is changing over time. For the sake of efficiency
     *     this information is computed exactly once. By default the methodology used will use a
     *     time-step that shrinks as the aircraft get closer together. If a fixed-time step is
     *     desired then manually call computeFixedTimeStepSeparationTimeSeries(Duration)
     */
    public SeparationTimeSeries separationInfo() {
        if (separationTimeSeriesIsMissing()) {
            computeDynamicSeparationTimeSeries();
        }
        checkNotNull(sepInfo, "The SeparationTimeSeries should now be set");
        return this.sepInfo;
    }

    /** @return True if the SeparationTimeSeries has NOT been computed yet. */
    public boolean separationTimeSeriesIsMissing() {
        return sepInfo == null;
    }

    /**
     * Compute, and cache, A SeparationTimeSeries computed using a custom methodology.
     *
     * @param methodology A Lambda function that can create a SeparationTimeSeries from a TrackPair
     */
    public void computeSeparationTimeSeries(Function<TrackPair, SeparationTimeSeries> methodology) {
        checkState(separationTimeSeriesIsMissing(), "The SeparationTimeSeries cannot be set twice");
        this.sepInfo = methodology.apply(this);
    }

    /**
     * Compute, and cache, A SeparationTimeSeries that uses a dynamic time step that depends on the
     * separation between the aircraft. As the aircraft gets closer the times series resolution gets
     * finer. But, when the aircraft are far apart the time series a uses big steps on the time-axis
     * for efficient computation.
     */
    public void computeDynamicSeparationTimeSeries() {
        computeSeparationTimeSeries(trackPair -> dynamicTimeStep(trackPair));
    }

    /**
     * Compute, and cache, A SeparationTimeSeries that uses a fixed time step.
     *
     * @param timeStep The time step to use. Smaller values may produce more accurate results at the
     *                 cost of more computation.
     */
    public void computeFixedTimeStepSeparationTimeSeries(Duration timeStep) {
        computeSeparationTimeSeries(trackPair -> new SeparationTimeSeries(trackPair, timeStep));
    }

    /**
     * @return The TimeWindow that covers the space of time for which there is data for both tracks
     *     space if the data from these two tracks overlap in time.
     */
    public Optional<TimeWindow> timeOverlap() {
        return track1.asTimeWindow().getOverlapWith(track2.asTimeWindow());
    }

    /**
     * @return True if the data from these two tracks overlaps in time.
     */
    public boolean overlapInTime() {
        return timeOverlap().isPresent();
    }

    /**
     * @param timeStep The duration of time between any two consecutive Instants in the returned
     *                 list.
     *
     * @return A sorted sequence of Instants that are all within the "time overlap" of these two
     *     Tracks.
     */
    public ArrayList<Instant> timesInOverlap(Duration timeStep) {
        return overlapInTime()
            ? timeOverlap().get().steppedIteration(timeStep)
            : newArrayList();
    }

    /**
     * For each Track create an interpolated Point at the provided time.
     *
     * @param time A time that occurs in the overlapping TimeWindow between these two tracks. (This
     *             is an interpolation, not an extrapolation)
     *
     * @return A Pair of Points that were created by interpolating the data from each Track.
     */
    public PointPair interpolatedPointsAt(Instant time) {
        checkNotNull(time);
        checkState(timeOverlap().isPresent(), "This method requires the Tracks to overlap");
        checkArgument(timeOverlap().get().contains(time), "The provided time must occur within the overlap");

        return PointPair.of(
            (Point) track1.interpolatedPoint(time).get(),
            (Point) track2.interpolatedPoint(time).get()
        );
    }

    /**
     * Returns True if these two tracks are ever separated by a fixed <b>lateral</b> distance (ie
     * altitude data is ignored). This method is much faster than using {@code maxDistBetween(t1,
     * t2) > distInNm} because this method usually returns a result without needing to iterate
     * across the entire track
     *
     * @param distInNm A distance in Nautical Miles
     *
     * @return True if these two tracks ever separated by this required distance. These tracks must
     *     overlap in time for this method to have any meaning.
     */
    public boolean separateBy(double distInNm) {
        checkState(overlapInTime(), "Value not defined because the tracks do not overlap in time");

        Duration TIME_STEP = Duration.ofSeconds(2);

        TimeWindow overlap = timeOverlap().get();
        Instant endTime = overlap.end();

        Instant currentTime = overlap.start();
        while (currentTime.isBefore(endTime)) {

            Optional<Point> opt1 = track1.interpolatedPoint(currentTime);
            Optional<Point> opt2 = track2.interpolatedPoint(currentTime);

            //set the "isClose" variable
            if (!opt1.isPresent() || !opt2.isPresent()) {
                throw new IllegalStateException("Both points should have viable interpolation");
            }

            LatLong location1 = opt1.get().latLong();
            LatLong location2 = opt2.get().latLong();

            if (location1.distanceInNM(location2) > distInNm) {
                return true;
            }

            currentTime = currentTime.plus(TIME_STEP);
        }
        return false;
    }

    /**
     * Rely on the precomputed SeparationTimeSeries to determine if these two aircraft come within
     * the specified lateral distance.
     *
     * @param lateralDistance
     *
     * @return True if these aircraft come at least this close
     */
    public boolean comeWithin(Distance lateralDistance) {
        Iterator<Instant> times = separationInfo().times();

        while (times.hasNext()) {
            Instant time = times.next();
            if (sepInfo.horizontalSeparationAt(time).isLessThanOrEqualTo(lateralDistance)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rely on the precomputed SeparationTimeSeries to determine if these two aircraft come within
     * the specified lateral and vertical distances.
     *
     * @param lateralDistance
     * @param verticalDistance
     *
     * @return True if these aircraft come at least this close
     */
    public boolean comeWithin(Distance lateralDistance, Distance verticalDistance) {

        for (Instant time : separationInfo()) {
            boolean horizontal = sepInfo.horizontalSeparationAt(time).isLessThanOrEqualTo(lateralDistance);
            boolean vertical = sepInfo.verticalSeparationAt(time).isLessThanOrEqualTo(verticalDistance);

            if (horizontal && vertical) {
                return true;
            }
        }
        return false;
    }

    public boolean overlapContains(Instant time) {
        requireNonNull(time);
        return overlapInTime() && timeOverlap().get().contains(time);
    }
}
