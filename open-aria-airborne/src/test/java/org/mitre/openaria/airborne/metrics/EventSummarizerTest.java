package org.mitre.openaria.airborne.metrics;

import static java.time.Instant.EPOCH;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.openaria.airborne.AirborneEventTest.testRecord;
import static org.mitre.caasd.commons.parsing.nop.Facility.A80;
import static org.mitre.caasd.commons.parsing.nop.Facility.D10;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class EventSummarizerTest {

    @Test
    public void eventsFromSameFacilityAndDateAreCombined() {
        EventSummarizer summarizer = new EventSummarizer();

        Instant time = EPOCH;

        summarizer.accept(testRecord(time, D10, 12.0));
        summarizer.accept(testRecord(time, D10, 21.0));
        summarizer.accept(testRecord(time, D10, 51.0));

        DailyMetrics summary = summarizer.getSummariesFor(time).get(D10);

        assertThat(summary.eventCount(), is(3));
        assertThat(summary.facility(), is(D10));
        assertThat(summary.date(), is("1970-01-01"));
        assertThat(summary.avgScore(), is(28.0));
    }

    @Test
    public void canCombineSeparateSummarizers() {
        EventSummarizer summarizer1 = new EventSummarizer();
        EventSummarizer summarizer2 = new EventSummarizer();
        EventSummarizer summarizer3 = new EventSummarizer();

        Instant time = EPOCH;

        summarizer1.accept(testRecord(time, D10, 12.0));
        summarizer2.accept(testRecord(time, D10, 21.0));
        summarizer3.accept(testRecord(time, A80, 51.0));

        EventSummarizer sink = new EventSummarizer();
        sink.ingestAll(summarizer1);
        sink.ingestAll(summarizer2);
        sink.ingestAll(summarizer3);

        DailyMetrics d10_summary = sink.getSummariesFor(time).get(D10);

        assertThat(d10_summary.eventCount(), is(2));
        assertThat(d10_summary.facility(), is(D10));
        assertThat(d10_summary.date(), is("1970-01-01"));
        assertThat(d10_summary.avgScore(), is(16.5));
        assertThat(d10_summary.histogram(), is(new int[]{0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));

        DailyMetrics a80_summary = sink.getSummariesFor(time).get(A80);

        assertThat(a80_summary.eventCount(), is(1));
        assertThat(a80_summary.facility(), is(A80));
        assertThat(a80_summary.date(), is("1970-01-01"));
        assertThat(a80_summary.avgScore(), is(51.0));
        assertThat(a80_summary.histogram(), is(new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
    }


    /**
     * Test of eventCount method, of class EventSummarizer.
     */
    @Test
    public void testEventCount() {

        EventSummarizer summarizer = new EventSummarizer();
        summarizer.accept(testRecord(EPOCH, D10, 10.0));
        assertThat(summarizer.eventCounts, is(1));

        summarizer.accept(testRecord(EPOCH, D10, 10.0));
        assertThat(summarizer.eventCounts, is(2));
    }

}
