package org.mitre.openaria.airborne.tools;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.TimeWindow;
import org.mitre.openaria.core.utils.Misc;

/**
 * A TimeDensityAuditor aggregates and summarizes Time measurements. It essentially builds a rolling
 * Histogram with a fixed "time bucket size" and a maximum number of frequency buckets. This can
 * help us understand a wide variety of datasets that have a time component.
 */
public class TimeDensityAuditor implements Consumer<Instant> {

    // "How big of a time bucket" should we have when we round/truncate time values
    private final Duration timeBucketSize;

    // Adapts timeBucketSize into a TemporalUnit so we can use Instant.truncatedTo(bucketSize)
    private final TemporalUnit bucketSize_asTemporalUnit;

    // How many time buckets should we keep around (may impact memory use when buckets are small)
    private final int maxNumTimeBuckets;

    // Where we actually keep the counts
    Map<Instant, Integer> freqPerTimeBucket = new TreeMap<>();


    public TimeDensityAuditor() {
        this(24 * 60, Duration.ofSeconds(60)); // by default track a maximum of 24 hours worth of minutes
    }

    TimeDensityAuditor(int maxNumTimeBuckets, Duration timeBucketSize) {
        checkArgument(maxNumTimeBuckets >= 1);
        requireNonNull(timeBucketSize);

        this.maxNumTimeBuckets = maxNumTimeBuckets;
        this.timeBucketSize = timeBucketSize;
        this.bucketSize_asTemporalUnit = adaptDuration(timeBucketSize);
    }

    private static TemporalUnit adaptDuration(final Duration duration) {
        /*
         * This minimal adaptation of a Duration into a TemporalUnit was implemented to match the
         * source code of Instant.truncatedTo(TemporalUnit) (which ONLY uses the getDuration()
         * method from the TemporalUnit interface)
         */
        return new TemporalUnit() {
            @Override
            public Duration getDuration() {
                return duration;
            }

            @Override
            public boolean isDurationEstimated() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDateBased() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isTimeBased() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <R extends Temporal> R addTo(R temporal, long amount) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
                throw new UnsupportedOperationException();
            }
        };
    }


    public Duration timeBucketSize() {
        return timeBucketSize;
    }

    @Override
    public void accept(Instant time) {
        incrementTimeBucketCounts(time);
        enforceBucketCap();
    }

    private void incrementTimeBucketCounts(Instant time) {

        Instant roundedTime = time.truncatedTo(bucketSize_asTemporalUnit);

        freqPerTimeBucket.merge(roundedTime, 1, (v1, v2) -> v1 + v2);
    }

    private void enforceBucketCap() {

        int bucketsToRemove = max(0, freqPerTimeBucket.size() - maxNumTimeBuckets);

        if (bucketsToRemove > 0) {
            Set<Instant> removeMe = freqPerTimeBucket.keySet().stream()
                .sorted()
                .limit(bucketsToRemove)
                .collect(Collectors.toSet());

            removeMe.forEach(instant -> freqPerTimeBucket.remove(instant));
        }
    }

    public Map<Instant, Integer> freqPerTimeBucket() {
        return freqPerTimeBucket;
    }

    /** @return The smallest possible TimeWindow that includes every Instant with a frequency count. */
    public TimeWindow spanningWindow() {
        return Misc.enclosingTimeWindow(freqPerTimeBucket.keySet());
    }

    /**
     * @return An Iterator that will return Instants from the "min instant" being track to the "max
     *     instant" being tracked.  All instants in between will also be included in the iteration
     *     (step size = size of time buckets)
     */
    public Iterator<Instant> bucketTimeIterator() {
        return spanningWindow().iterator(timeBucketSize);
    }
}
