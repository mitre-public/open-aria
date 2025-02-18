
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.newLinkedList;
import static java.util.Arrays.binarySearch;
import static org.mitre.caasd.commons.CollectionUtils.zip;
import static org.mitre.caasd.commons.Time.confirmTimeOrdering;
import static org.mitre.openaria.core.Interpolate.interpolate;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.Triple;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * A SeparationTimeSeries contains information about how far two aircraft are at any given time.
 * <p>
 * The goal of a SeparationTimeSeries is to make querying TrackPair information easier
 */
public class SeparationTimeSeries implements Serializable, Iterable<Instant> {

    /** MinTime to MaxTime. */
    private final TimeWindow timeWindow;

    /**
     * A strictly increasing sequence of Instants in which there is location data for both aircraft.
     * This array is often the basis of binary searches.
     */
    private final Instant[] times;

    /** The Vertical Distance measurements taken at the times recorded in the times array. */
    private final Distance[] verticalDistances;

    /** The Horizontal Distance measurements taken at the times recorded in the times array. */
    private final Distance[] horizontalDistances;

    public SeparationTimeSeries(Instant[] times, Distance[] verticalDistances, Distance[] horizontalDistances) {
        this.times = checkNotNull(times);
        checkArgument(times.length > 1);
        this.verticalDistances = checkNotNull(verticalDistances);
        this.horizontalDistances = checkNotNull(horizontalDistances);
        checkArgument(times.length == verticalDistances.length);
        checkArgument(times.length == horizontalDistances.length);
        confirmTimeOrdering(times); //fail if the times aren't properly ordered
        this.timeWindow = TimeWindow.of(times[0], times[times.length - 1]);
    }

    public SeparationTimeSeries(TrackPair<?> trackPair, Duration timeStep) {
        checkNotNull(trackPair);
        checkArgument(trackPair.overlapInTime());
        checkNotNull(timeStep);
        checkArgument(!timeStep.isNegative(), "The timeStep cannot be negative");
        checkArgument(!timeStep.isZero(), "The timeStep cannot be zero");

        this.times = trackPair.timesInOverlap(timeStep).toArray(new Instant[0]);
        checkArgument(times.length > 1);
        this.timeWindow = TimeWindow.of(times[0], times[times.length - 1]);
        this.verticalDistances = new Distance[times.length];
        this.horizontalDistances = new Distance[times.length];

        fillInSeparationTimeSeries(trackPair);
    }

    public static Duration DEFAULT_TIME_STEP = Duration.ofSeconds(5);

    /**
     * Create a SeparationTimeSeries that uses a dynamic time step. The time step is smallest when
     * the aircraft are within 10 nautical miles. The time step is largest when the aircraft are
     * further than 50 nautical miles apart.
     *
     * @param trackPair Two Tracks
     *
     * @return The SeparationTimeSeries that describes the interaction btw these two Tracks
     */
    public static SeparationTimeSeries dynamicTimeStep(TrackPair<?> trackPair) {
        checkNotNull(trackPair);
        checkArgument(trackPair.overlapInTime());

        TimeWindow overlap = trackPair.timeOverlap().get();

        Instant startTime = overlap.start();
        Instant endTime = overlap.end();

        record Triple(Instant time, Distance horizontal, Distance vertical) {}

        LinkedList<Triple> triples = newLinkedList();

        Instant curTime = startTime;
        while (curTime.isBefore(endTime)) {

            PointPair points = trackPair.interpolatedPointsAt(curTime);
            Distance horizontal = points.lateralDistance();
            Distance vert = points.altitudeDelta();

            Triple trip = new Triple(curTime, horizontal, vert);

            triples.add(trip);

            Duration timeStep = getDynamicTimeStep(horizontal);

            curTime = curTime.plus(timeStep);
        }

        //add the final "end of time window" triplet
        if (!triples.getLast().time.equals(endTime)) {
            PointPair points = trackPair.interpolatedPointsAt(endTime);
            Distance horizontal = points.lateralDistance();
            Distance vert = points.altitudeDelta();

            triples.add(new Triple(endTime, horizontal, vert));
        }

        Instant[] times = triples.stream()
            .map(triple -> triple.time)
            .toList()
            .toArray(new Instant[0]);

        Distance[] vertical = triples.stream()
            .map(triple -> triple.vertical)
            .toList()
            .toArray(new Distance[0]);

        Distance[] horizontal = triples.stream()
            .map(triple -> triple.horizontal)
            .toList()
            .toArray(new Distance[0]);

        return new SeparationTimeSeries(times, vertical, horizontal);
    }

    private static final Distance FIFTEEN_MILES = Distance.ofNauticalMiles(15);

    private static final Distance THIRTY_MILES = Distance.ofNauticalMiles(30);

    private static final Distance FIFTY_MILES = Distance.ofNauticalMiles(50);

    private static Duration getDynamicTimeStep(Distance horizontalDist) {
        if (horizontalDist.isLessThan(FIFTEEN_MILES)) {
            return Duration.ofMillis(2_500);
        } else if (horizontalDist.isLessThan(THIRTY_MILES)) {
            return Duration.ofMillis(5_000);
        } else if (horizontalDist.isLessThan(FIFTY_MILES)) {
            return Duration.ofMillis(10_000);
        } else {
            return Duration.ofMillis(20_000);
        }
    }

    public TimeWindow timeWindow() {
        return this.timeWindow;
    }

    public UnmodifiableIterator<Instant> times() {
        return Iterators.forArray(times);
    }

    /**
     * Lookup (and typically interpolate) the vertical separation at a particular moment in time.
     *
     * @param time An instant in time on the timeline of these time series
     *
     * @return The vertical separation at this moment in time
     */
    public Distance verticalSeparationAt(Instant time) {
        return separationAt(verticalDistances, time);
    }

    /**
     * Lookup (and typically interpolate) the horizontal separation at a particular moment in time.
     *
     * @param time An instant in time on the timeline of these time series
     *
     * @return The horizontal separation at this moment in time
     */
    public Distance horizontalSeparationAt(Instant time) {
        return separationAt(horizontalDistances, time);
    }

    /**
     * @param distArray A loop-up table of known distances (one entry for each entry in the
     *                  "Instant[] times" field)
     * @param time      The lookup time
     *
     * @return The interpolated Distance
     */
    private Distance separationAt(Distance[] distArray, Instant time) {
        checkArgument(timeWindow.contains(time));

        int index = binarySearch(times, time);

        //perfect match to time value in times array
        if (index >= 0) {
            return distArray[index];
        }

        //binary search didn't match, but it did give insertion point
        int insertionPoint = -index - 1;

        TimeWindow window = TimeWindow.of(times[insertionPoint - 1], times[insertionPoint]);

        double frac = window.toFractionOfRange(time);

        return interpolate(
            distArray[insertionPoint - 1],
            distArray[insertionPoint],
            frac
        );
    }

    private void fillInSeparationTimeSeries(TrackPair<?> trackPair) {
        for (int i = 0; i < times.length; i++) {
            PointPair points = trackPair.interpolatedPointsAt(times[i]);
            verticalDistances[i] = points.altitudeDelta();
            horizontalDistances[i] = points.lateralDistance();
        }
    }

    /**
     * The Speed at which the Vertical Separation is changing at a particular moment in time.
     *
     * @param time An instant in time on the timeline of these time series
     *
     * @return A positive speed if the separation is getting smaller, a negative speed the
     *     separation is getting larger.
     */
    public Speed verticalClosureRateAt(Instant time) {
        checkArgument(timeWindow.contains(time));
        return closureRateAt(verticalDistances, time);
    }

    /**
     * The Speed at which the Horizontal Separation is changing at a particular moment in time.
     *
     * @param time An instant in time on the timeline of these time series
     *
     * @return A positive speed if the separation is getting smaller, a negative speed the
     *     separation is getting larger.
     */
    public Speed horizontalClosureRateAt(Instant time) {
        checkArgument(timeWindow.contains(time));
        return closureRateAt(horizontalDistances, time);
    }

    private Speed closureRateAt(Distance[] distArray, Instant time) {
        checkArgument(timeWindow.contains(time));

        int index = binarySearch(times, time);

        int adjustedIndex = (index >= 0)
            ? index //time was an exact match to one of the Instants in the times arrary
            : -index - 2; //the time comes between two Instants in the times array

        return closureRateAtArrayIndex(distArray, adjustedIndex);
    }

    private Speed closureRateAtArrayIndex(Distance[] distArray, int index) {
        //prevent ArrayIndexOutOfBoundsException at the last index
        int adjustedIndex = (index == times.length - 1)
            ? times.length - 2
            : index;

        Distance earlyDistance = distArray[adjustedIndex];
        Distance laterDistance = distArray[adjustedIndex + 1];

        Distance distDelta = earlyDistance.minus(laterDistance);
        Duration timeDelta = Duration.between(times[adjustedIndex], times[adjustedIndex + 1]);

        checkState(!timeDelta.isNegative());

        return new Speed(distDelta, timeDelta);
    }

    /**
     * Given the vertical separation and vertical closure rate at this instant in time estimate the
     * amount of time it will take for the vertical separation to shrink to zero.
     *
     * @param time An instant in time on the timeline of these time series
     *
     * @return An estimate of how long it will take for the vertical separation to be zero. This
     *     assumes vertical separation is going decreasing at this moment in time. Return null if
     *     this assumption is false and the vertical separation is not decreasing.
     */
    public Optional<Duration> timeUntilVerticalClosure(Instant time) {
        checkArgument(timeWindow.contains(time));
        return timeUntilClosure(verticalDistances, time);
    }

    /**
     * Given the horizontal separation and horizontal closure rate at this instant in time estimate
     * the amount of time it will take for the horizontal separation to shrink to zero.
     *
     * @param time An instant in time on the timeline of these time series
     *
     * @return An estimate of how long it will take for the vertical separation to be zero. This
     *     assumes vertical separation is going decreasing at this moment in time. Return null if
     *     this assumption is false and the vertical separation is not decreasing.
     */
    public Optional<Duration> timeUntilHorizontalClosure(Instant time) {
        checkArgument(timeWindow.contains(time));
        return timeUntilClosure(horizontalDistances, time);
    }

    /*
     * Estimate how long it will take to reach "zero separation" given the separation and closure
     * rate at this moment in time. This method returns null if the distances are not shrinking at
     * this moment in time.
     */
    private Optional<Duration> timeUntilClosure(Distance[] distArray, Instant time) {
        Distance currentDistance = separationAt(distArray, time);
        Speed closureRate = closureRateAt(distArray, time);

        return (closureRate.isPositive())
            ? Optional.of(closureRate.timeToTravel(currentDistance))
            : Optional.empty();
    }

    public Optional<Distance> verticalDistAtHorizontalClosureTime(Instant time) {
        Optional<Duration> timeUntilClosure = timeUntilHorizontalClosure(time);

        //not closing in the horizontal direction
        if (!timeUntilClosure.isPresent()) {
            return Optional.empty();
        }

        Speed closureRate = verticalClosureRateAt(time);
        Distance startingSeparation = verticalSeparationAt(time);
        Distance distanceClosed = closureRate.times(timeUntilClosure.get());

        return Optional.of(startingSeparation.minus(distanceClosed).abs());
    }

    public Optional<Distance> horizontalDistAtVerticalClosureTime(Instant time) {
        Optional<Duration> timeUntilClosure = timeUntilVerticalClosure(time);

        //not closing in the vertical direction
        if (!timeUntilClosure.isPresent()) {
            return Optional.empty();
        }

        Speed closureRate = horizontalClosureRateAt(time);
        Distance startingSeparation = horizontalSeparationAt(time);
        Distance distanceClosed = closureRate.times(timeUntilClosure.get());

        return Optional.of(startingSeparation.minus(distanceClosed).abs());
    }

    /**
     * Given the vertical closure rate at the provided time predict the vertical separation at a
     * moment in the near future.
     *
     * @param curTime  The current moment in time (which provides the close rate and the starting
     *                 separation distance)
     * @param timeStep The amount of time to travel at the closure rate
     *
     * @return The predicated vertical separation
     */
    public Distance predictedVerticalSeparation(Instant curTime, Duration timeStep) {
        return predictedSeparation(verticalDistances, curTime, timeStep);
    }

    /**
     * Given the horizontal closure rate at the provided time predict the horizontal separation at a
     * moment in the near future.
     *
     * @param curTime  The current moment in time (which provides the close rate and the starting
     *                 separation distance)
     * @param timeStep The amount of time to travel at the closure rate
     *
     * @return The predicated horizontal separation
     */
    public Distance predictedHorizontalSeparation(Instant curTime, Duration timeStep) {
        return predictedSeparation(horizontalDistances, curTime, timeStep);
    }

    public Distance predictedSeparation(Distance[] distArray, Instant curTime, Duration timeStep) {
        checkNotNull(curTime);
        checkNotNull(timeStep);
        checkArgument(!timeStep.isNegative());

        Speed closureRate = closureRateAt(distArray, curTime);
        Distance distanceClosed = closureRate.times(timeStep);

        Distance startingSeparation = separationAt(distArray, curTime);

        //closure rate is positive when the separation is falling -- so use minus, not plus
        return startingSeparation.minus(distanceClosed).abs();
    }

    public Pair<Distance, Instant> minimumHorizontalSeparation() {

        Distance minDistance = horizontalDistances[0];
        Instant timeOfMin = times[0];

        for (int i = 1; i < horizontalDistances.length; i++) {
            if (horizontalDistances[i].isLessThan(minDistance)) {
                minDistance = horizontalDistances[i];
                timeOfMin = times[i];
            }
        }

        return Pair.of(minDistance, timeOfMin);
    }

    public Iterator<Triple<Instant, Distance, Distance>> timeLatVertIterator() {
        return zip(
            Iterators.forArray(times),
            Iterators.forArray(horizontalDistances),
            Iterators.forArray(verticalDistances)
        );
    }

    public static Comparator<Triple<Instant, Distance, Distance>> lateralComparator() {

        return (Triple<Instant, Distance, Distance> o1, Triple<Instant, Distance, Distance> o2)
            -> o1.second().compareTo(o2.second());
    }

    public static Comparator<Triple<Instant, Distance, Distance>> verticalComparator() {

        return (Triple<Instant, Distance, Distance> o1, Triple<Instant, Distance, Distance> o2) ->
            o1.third().compareTo(o2.third());
    }

    @Override
    public Iterator<Instant> iterator() {
        return times();
    }
}
