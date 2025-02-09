package org.mitre.openaria.airborne.tools;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class TimeDensityAuditorTest {

    Instant t1 = EPOCH.plusSeconds(1);
    Instant t2 = EPOCH.plusSeconds(70);
    Instant t3 = EPOCH.plusSeconds(71);

    @Test
    void countsAggregateWithinBucket() {

        TimeDensityAuditor auditor = new TimeDensityAuditor();

        auditor.accept(t1);
        auditor.accept(t2);
        auditor.accept(t3);

        assertThat(auditor.freqPerTimeBucket().keySet(), hasSize(2));
        assertThat(auditor.freqPerTimeBucket().containsKey(EPOCH), is(true));
        assertThat(auditor.freqPerTimeBucket().containsKey(EPOCH.plusSeconds(60)), is(true));

        assertThat(auditor.freqPerTimeBucket().get(EPOCH), is(1));
        assertThat(auditor.freqPerTimeBucket().get(EPOCH.plusSeconds(60)), is(2));
    }

    @Test
    void smallBucketsChangeAggregation() {
        TimeDensityAuditor auditor = new TimeDensityAuditor(100, Duration.ofSeconds(1));

        auditor.accept(t1);
        auditor.accept(t2);
        auditor.accept(t3);

        assertThat(auditor.freqPerTimeBucket().keySet(), hasSize(3));
        assertThat(auditor.freqPerTimeBucket().containsKey(EPOCH.plusSeconds(1)), is(true));
        assertThat(auditor.freqPerTimeBucket().containsKey(EPOCH.plusSeconds(70)), is(true));
        assertThat(auditor.freqPerTimeBucket().containsKey(EPOCH.plusSeconds(71)), is(true));
    }

    @Test
    void oldBucketsGetEvicted() {
        // This "1" means we only track 1 bucket at a time.  So we better evict the old buckets
        TimeDensityAuditor auditor = new TimeDensityAuditor(1, Duration.ofSeconds(1));

        auditor.accept(t1);
        auditor.accept(t2);
        auditor.accept(t3);

        assertThat(auditor.freqPerTimeBucket().keySet(), hasSize(1));
        assertThat(auditor.freqPerTimeBucket().containsKey(EPOCH.plusSeconds(71)), is(true));
    }

}