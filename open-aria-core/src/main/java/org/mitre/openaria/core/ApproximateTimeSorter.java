

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.Time.theDuration;

import java.time.Duration;
import java.time.Instant;
import java.util.PriorityQueue;
import java.util.function.Consumer;

import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.Time;

/**
 * An {@code ApproximateTimeSorter<T>} intercepts and organizes timestamped-data that is eventually
 * delivered to a wrapped (i.e., downstream) Consumer<T>. An ApproximateTimeSorter reduces the
 * likelihood the wrapped Consumer<T> receives data that is out of time-order.
 * <p>
 * This partial protection is provided by temporarily storing all input data and only
 * emitting/releasing time-sorted data to the wrapped Consumer<T> when (A) the time difference
 * between the oldest and newest datum in temporary storage is too large or (B) the time difference
 * between the oldest datum and the most recent input datum is too large. Rule (A) ensures that an
 * ApproximateTimeSorter will not fail (due to an OutOfMemoryException) when the input data stream
 * goes backwards in time. Rule (B) corrects more patterns of out-of-order data but this rule alone
 * can leave data stranded in the ApproximateTimeSorter if all data moves backwards in time.
 * Temporarily storing and sorting data in this way will correct minor imperfection in the
 * time-ordering of the input data.
 * <p>
 * An ApproximateTimeSorter intercepts data that are intended for the Consumer supplied at
 * construction. When an ApproximateTimeSorter's accept(T) method is called the input datum is
 * intercepted and added to a "short term pool". Additionally, all previously intercepted data that
 * are "too old" <B> with respect to the oldest datum </B> are released to the Consumer supplied at
 * construction. Thus, the ApproximateTimeSorter has "filtered" the stream of data being delivered
 * to the Consumer.
 * <p>
 * Note, a ApproximateTimeSorter is only intended to correct minor imperfections in the time
 * ordering of a data-stream. For example, a ApproximateTimeSorter might hold upto 15 minutes worth
 * of data to ensure "small hiccups" in sort order are corrected. An ApproximateTimeSorter cannot
 * make stronger guarantees about the time-based sort order of a data stream because it is
 * impossible to know/hold the entire stream (which could be infinite).
 * <p>
 * An ApproximateTimeSorter operates under the assumption that the time of the i_th incoming datum
 * has an expected value that grows at a constant rate but a fixed standard deviation or "noise
 * level". For example, this distribution meets this expectation: Time_of_ith_Point ~
 * Normal(i*c,sigma) (where c and sigma are fixed constants).
 * <p>
 * An ApproximateTimeSorter can be combined with a StrictTimeSortEnforcer to ensure a Consumer that
 * absolutely requires time sorted input never receives out-of-order input.
 *
 * @see StreamingTimeSorter
 */
public class ApproximateTimeSorter<T extends HasTime> implements Consumer<T> {

    /**
     * The maximum amount of "input lag" that will be corrected by this ApproximateTimeSorter. As
     * this value grows so too does the number of record this ApproximateTimeSorter must retain to
     * meet its commitment to the user.
     */
    private final Duration regularEvictionLag;

    /**
     * Records that are this old with respect to the timeHighWaterMark are immediately evicted. This
     * guardrail prevents OutOfMemoryException when input data gradually gets OLDER and OLDER
     * instead of "newer and newer"
     */
    private final Duration failSafeEvictionLag;

    private final Consumer<T> outputMechanism;

    private final PriorityQueue<T> shortTermStorage;

    private Instant timeHighWaterMark;

    /**
     * The highest number of records ever held in this ApproximateTimeSorter's short term storage.
     * This value gives insight into how much memory is used when filtering input using a
     * ApproximateTimeSorter.
     */
    private int sizeHighWaterMark;

    public ApproximateTimeSorter(Duration maxInputLag, Consumer<T> outputMechanism) {
        this.regularEvictionLag = checkNotNull(maxInputLag);
        this.failSafeEvictionLag = maxInputLag.multipliedBy(2);
        this.outputMechanism = checkNotNull(outputMechanism);
        checkArgument(!maxInputLag.isNegative());
        this.shortTermStorage = new PriorityQueue<>();
        this.sizeHighWaterMark = 0;
    }

    @Override
    public void accept(T t) {
        updateTimeHighWaterMark(t.time());

        shortTermStorage.add(t);

        drainDueToLatestInput(t); //standard drain policy
        drainDueToTimeHighWaterMark();  //prevent blow-up when data goes backwards in time

        sizeHighWaterMark = Math.max(sizeHighWaterMark, shortTermStorage.size());
    }

    private void updateTimeHighWaterMark(Instant time) {
        timeHighWaterMark = (timeHighWaterMark == null)
            ? time
            : Time.latest(timeHighWaterMark, time);
    }

    /** Drains all data that is "too old" in comparison to a recent input t. */
    private void drainDueToLatestInput(T t) {
        while (!shortTermStorage.isEmpty() && shouldEvict(t.time(), regularEvictionLag)) {
            outputMechanism.accept(shortTermStorage.poll());
        }
    }

    /** Drains all data that is "too old" in comparison to the timeHighWaterMark. */
    private void drainDueToTimeHighWaterMark() {
        while (!shortTermStorage.isEmpty() && shouldEvict(timeHighWaterMark, failSafeEvictionLag)) {
            outputMechanism.accept(shortTermStorage.poll());
        }
    }

    /** @return True if the oldest point in storage is "older" than comparisonTime - evictionLimit. */
    private boolean shouldEvict(Instant comparisonTime, Duration evictionLimit) {
        T oldest = shortTermStorage.peek();
        Duration age = Duration.between(oldest.time(), comparisonTime);
        return theDuration(age).isGreaterThan(evictionLimit);
    }

    /** Drains the internal queue of data to the wrapped Consumer. */
    public void flush() {
        while (!shortTermStorage.isEmpty()) {
            T oldestRecord = shortTermStorage.poll();
            outputMechanism.accept(oldestRecord);
        }
    }

    /**
     * @return The number of records current held in the queue that have not yet been delivered to
     *     the outputMechanism (i.e. The Consumer<T>).
     */
    public int numRecordsInQueue() {
        return shortTermStorage.size();
    }

    /**
     * @return The highest number of records ever held in short term storage. This value gives
     *     insight into how much memory is used while organizing input destined for the Consumer.
     */
    public int sizeHighWaterMark() {
        return this.sizeHighWaterMark;
    }
}
