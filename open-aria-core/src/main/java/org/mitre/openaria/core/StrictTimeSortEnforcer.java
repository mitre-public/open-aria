
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newTreeMap;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.YyyyMmDd;

import com.google.common.math.StatsAccumulator;

/**
 * A StrictTimeSortEnforcer is a decorator that ensure that the wrapped Consumer<Point> receives
 * Point that are strictly sorted by time. This guarantee is met by aggressively filtering out input
 * Points data that are forwarded to the wrapped Consumer<Point>.
 * <p>
 * A StrictTimeSortEnforcer maintains a "currentTime" which is latest occurring time of all Points
 * it has consumed via the accept method. Anytime an input point occurs before the "currentTime"
 * (i.e. in the past) a warning is issued. If an input point occurs in the past it is "dropped".
 * Dropping an input point means (1) the "currentTime" field of this StrictTimeSortEnforcer is not
 * changed, (2) the dropped point is not forwarded to the wrapped Consumer<Point>, and (3) the
 * dropped point is made available via a call to "getLastDroppedPoint()".
 * <p>
 * Note, an ApproximateTimeSorter can be combined with a StrictTimeSortEnforcer to ensure a Point
 * consumer that absolutely requires time sorted input never receives out-of-order input.
 */
public class StrictTimeSortEnforcer<T extends HasTime> implements Consumer<T> {

    /** This time only increases, thus, currentTime is actually the "time high-water mark" */
    protected Instant currentTime;

    private T lastDroppedPoint;

    protected final Consumer<T> targetPointConsumer;

    /**
     * This Point Consumer receives Points that CANNOT be forwarded to the target Point Consumer
     * because they are out of chronological order.
     */
    private final Consumer<T> rejectedPointHandler;

    /**
     * Each StatsAccumulator summarizes "time steps taken" for single day. The StatsAccumulator
     * receive the delta between the "last point" and the "next new point" that increments the
     * "currentTime" value.
     */
    private final Map<YyyyMmDd, StatsAccumulator> timeIncrementsInMs;

    /**
     * Create a StrictTimeSortEnforcer that protects the targetPointConsumer from out-of-order
     * input. All Points that are not in chronologically increasing order are ignored. Be warned
     * that this behavior can be too brittle for some use-cases because it is possible for the
     * sequence of Points forwarded to the target consumer to be "broken" by one Point that has a
     * large time value.
     *
     * @param targetPointConsumer This consumer will not receive out-of-order input.
     */
    public StrictTimeSortEnforcer(Consumer<T> targetPointConsumer) {
        this(targetPointConsumer, ignorePoints());
    }

    /**
     * Create a StrictTimeSortEnforcer that protects the targetPointConsumer from out-of-order
     * input.
     *
     * @param targetPointConsumer  This consumer will not receive out-of-order input.
     * @param rejectedPointHandler This consumer receives the Points that could not be forwarded to
     *                             the target Point Consumer because they were out of chronological
     *                             order. This Consumer can throw warnings, gather metrics about how
     *                             much data is dropped, or merely do nothing.
     */
    public StrictTimeSortEnforcer(Consumer<T> targetPointConsumer, Consumer<T> rejectedPointHandler) {
        this.targetPointConsumer = checkNotNull(targetPointConsumer, "Must provide a downstream Point consumer");
        this.rejectedPointHandler = checkNotNull(rejectedPointHandler, "Must provide an rejected Point Handler");
        this.timeIncrementsInMs = newTreeMap();
    }

    @Override
    public void accept(T newPoint) {

        considerAdvancingCurrentTime(newPoint.time());

        if (newPoint.time().isBefore(currentTime)) {
            lastDroppedPoint = newPoint;
            rejectedPointHandler.accept(newPoint);
        } else {
            targetPointConsumer.accept(newPoint);
        }
    }

    private void considerAdvancingCurrentTime(Instant candidateTime) {
        if (currentTime == null) {
            currentTime = candidateTime;
            return;
        }

        //currentTime never goes backward...only adopt the new value if it occurs in the future
        if (candidateTime.isAfter(currentTime)) {
            logTimeIncrement(candidateTime);
            currentTime = candidateTime;
        }
    }

    private void logTimeIncrement(Instant candidateTime) {
        long increment = candidateTime.toEpochMilli() - currentTime.toEpochMilli();
        accumlatorFor(candidateTime).add(increment);
    }

    private StatsAccumulator accumlatorFor(Instant time) {

        return timeIncrementsInMs.computeIfAbsent(
            YyyyMmDd.from(time),
            (ignoreMe) -> new StatsAccumulator()
        );
    }

    public Map<YyyyMmDd, StatsAccumulator> timeIncrementsInMs() {
        return this.timeIncrementsInMs;
    }

    public Instant currentTime() {
        return this.currentTime;
    }

    public T getLastDroppedPoint() {
        return this.lastDroppedPoint;
    }

    static <T> Consumer<T> ignorePoints() {
        return (T p) -> {
            //do nothing
        };
    }
}
