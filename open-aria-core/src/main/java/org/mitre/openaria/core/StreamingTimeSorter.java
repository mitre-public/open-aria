package org.mitre.openaria.core;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newTreeMap;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.YyyyMmDd;

import com.google.common.math.StatsAccumulator;

/**
 * A StreamingTimeSorter<T> intercepts, and organizes, time-stamped data that is eventually
 * delivered to a Consumer<T>. A StreamingTimeSorter ensures the wrapped Consumer<T> <B>never</B>
 * receives out-of-order input (assuming all input data is routed through this
 * StreamingTimeSorter).
 *
 * <p>A StreamingTimeSorter provides this guarantee by combining the capabilities of a {@link
 * ApproximateTimeSorter} and a {@link StrictTimeSortEnforcer}.
 *
 * @see org.mitre.openaria.core.ApproximateTimeSorter
 * @see org.mitre.openaria.core.StrictTimeSortEnforcer
 */
public class StreamingTimeSorter<T extends HasTime> implements Consumer<T> {

    /* Strictly ensures all data passed to the targetConsumer are in chronological-order. */
    private final StrictTimeSortEnforcer<T> strictSorter;

    /**
     * Maintains an in-memory buffer of data to improve time sequencing before forwarding data
     * to the StrictTimeSortEnforcer.
     */
    private final ApproximateTimeSorter<T> approxSorter;

    private final StreamIntegritySummarizer<T> integritySummarizer;

    /**
     * Create a StreamingTimeSorter that ensures the wrapped downstreamConsumer never receives
     * out-of-time-order data (assuming all data is routed through this sorter).
     *
     * @param target          This Consumer<T> will receive a time-ordered stream of data
     * @param sortingDuration How much data is kept in memory to smooth out timing errors
     * @param rejectedData    This Consumer<T> receives all data that is rejected because they were
     *                        so far out of time-order that the in-memory data buffer could not
     *                        correct the timing difference.
     */
    public StreamingTimeSorter(Consumer<T> target, Duration sortingDuration, Consumer<T> rejectedData) {
        this.integritySummarizer = new StreamIntegritySummarizer<>();

        Consumer<T> droppedInputHandler = rejectedData.andThen(integritySummarizer);

        this.strictSorter = new StrictTimeSortEnforcer<>(target, droppedInputHandler);
        this.approxSorter = new ApproximateTimeSorter<>(sortingDuration, strictSorter);
    }

    /**
     * Create a StreamingTimeSorter that ensures the wrapped downstreamConsumer never receives
     * out-of-time-order data (assuming all data is routed through this sorter).
     *
     * @param target          This Consumer<T> will receive a time-ordered stream of data
     * @param sortingDuration How much data is kept in memory to smooth out timing errors
     */
    public StreamingTimeSorter(Consumer<T> target, Duration sortingDuration) {
        this(target, sortingDuration, ignoredInput -> {});
    }

    @Override
    public void accept(T t) {
        /*
         * The integritySummarizer's regular "accept" method keeps track of rejected points. Here we
         * track ALL point before any rejection logic is applied.
         */
        integritySummarizer.acceptUnsortedRecords(t);
        approxSorter.accept(t);
    }

    /**
     * @return The time of the last datum this StreamingTimeSorter sent to its target Consumer. The
     *     time value returned here should be about "one buffer length" behind the times of the data
     *     flowing into this StreamingTimeSorter. Note: this method returns null if no data has been
     *     sent to the target Consumer.
     */
    public Instant timeSeenByTarget() {
        //Note, this time can AND SHOULD be null when no data has been sent to the target consumer.
        return strictSorter.currentTime();
    }

    /**
     * Flush the in-memory data buffer, thus ensuring all data is processed because no data was
     * stranded in memory.
     */
    public void flush() {
        this.approxSorter.flush();
    }

    public ApproximateTimeSorter<T> inMemoryBuffer() {
        return this.approxSorter;
    }

    public StreamIntegritySummarizer<T> integritySummarizer() {
        return this.integritySummarizer;
    }

    /**
     * @return A Map that provides statistics on the FORWARD ONLY time increments. This is used to
     *     detect forward jumps in the data (i.e. likely data gaps)
     */
    public Map<YyyyMmDd, StatsAccumulator> timeIncrementsInMs() {
        return this.strictSorter.timeIncrementsInMs();
    }

    /** Keeps Track of (1) Total input count, (2) Delay of input records that had to be thrown out. */
    public class StreamIntegritySummarizer<T extends HasTime> implements Consumer<T> {

        /**
         * Each StatsAccumulator summarizes "delay data" for single day. The StatsAccumulator
         * receive the delay between the "currentTime" and the time of an input recrod that was
         * dropped because it was out of chronological order.
         */
        private final Map<YyyyMmDd, StatsAccumulator> droppedPointDelays;

        private final Map<YyyyMmDd, Long> unsortedInputCounts;

        private StreamIntegritySummarizer() {
            this.droppedPointDelays = newTreeMap();
            this.unsortedInputCounts = newTreeMap();
        }

        private void acceptUnsortedRecords(T p) {
            YyyyMmDd date = YyyyMmDd.from(p.time());
            unsortedInputCounts.merge(date, 1L, Long::sum); //insert 1 if missing, otherwise increment by 1
        }

        @Override
        public void accept(T delayedInputRecord) {
            long delayInMs = strictSorter.currentTime.toEpochMilli() - delayedInputRecord.time().toEpochMilli();

            //save statistics on the delay amount
            accumlatorFor(delayedInputRecord).add(delayInMs);
        }

        /* Get or create the StatsAccumulator that will be used to summarize rejected data. */
        private StatsAccumulator accumlatorFor(T t) {

            return droppedPointDelays.computeIfAbsent(
                YyyyMmDd.from(t.time()),
                (ignoreMe) -> new StatsAccumulator()
            );
        }

        public Map<YyyyMmDd, StatsAccumulator> droppedPointData() {
            return droppedPointDelays;
        }

        public Map<YyyyMmDd, Long> completePointCounts() {
            return unsortedInputCounts;
        }

        public long totalInputCount() {
            return unsortedInputCounts.entrySet().stream()
                .mapToLong(entry -> entry.getValue())
                .sum();
        }

        public long droppedCount() {
            return droppedPointDelays.entrySet().stream()
                .mapToLong(entry -> entry.getValue().count())
                .sum();
        }

        public long totalDelayOfDroppedRecordsInMs() {
            return droppedPointDelays.entrySet().stream()
                .mapToLong(entry -> (long) entry.getValue().sum())
                .sum();
        }
    }

    //THIS SHOULD BE MOVED SOMEWHERE ELSE
    public static <K, V> Map<K, V> mergeMaps(Collection<Map<K, V>> maps, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {

        HashMap<K, V> combined = newHashMap();

        for (Map<K, V> map : maps) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                combined.merge(entry.getKey(), entry.getValue(), remappingFunction);
            }
        }

        return combined;
    }
}
