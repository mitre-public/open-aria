package org.mitre.openaria.airborne.metrics;

import static java.time.Instant.EPOCH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.airborne.AirborneEventTest.testRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.mitre.openaria.airborne.AirborneEvent;

import org.junit.jupiter.api.Test;

public class DailyMetricsTest {

    @Test
    public void noargConstructorMakesInvalidObject() {
        DailyMetrics empty = new DailyMetrics();

        assertThrows(
            Exception.class,
            () -> empty.validate()
        );
    }

    @Test
    public void oneEventConstructorWorksAsExpected() {
        DailyMetrics obj = new DailyMetrics(testRecord(
            Instant.EPOCH,
            19.0
        ));

        assertThat(obj.date(), is("1970-01-01"));
        assertThat(obj.eventCount(), is(1));
        assertThat(obj.avgScore(), is(19.0));
        assertThat(obj.histogram().length, is(DailyMetrics.NUM_HISTOGRAM_BINS));
        assertTrue(
            Arrays.equals(obj.histogram(),
                new int[]{0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0})
        );
    }

    @Test
    public void canIntegrateNewEventIntoAMetricsObject() {
        AirborneEvent event1 = testRecord(Instant.EPOCH, 22.0);
        AirborneEvent event2 = testRecord(Instant.EPOCH, 18.0);

        DailyMetrics one = new DailyMetrics(event1);
        DailyMetrics two = new DailyMetrics(one, event2);

        assertThat(two.date(), is("1970-01-01"));
        assertThat(two.eventCount(), is(2));
        assertThat(two.avgScore(), is(20.0));
        assertThat(two.histogram().length, is(DailyMetrics.NUM_HISTOGRAM_BINS));
        assertTrue(Arrays.equals(two.histogram(),
            new int[]{0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
    }

    @Test
    public void willRejectNonMatchingDate() {

        AirborneEvent event1 = testRecord(Instant.EPOCH, 22.0);
        AirborneEvent event2 = testRecord(Instant.EPOCH.plus(Duration.ofDays(2)), 18.0);

        DailyMetrics one = new DailyMetrics(event1);

        assertThrows(
            IllegalArgumentException.class,
            () -> new DailyMetrics(one, event2),
            "should fail because dates dont match"
        );
    }

    private static DailyMetrics testExampleAsObject() {
        return new DailyMetrics(EPOCH.plus(Duration.ofHours(36)), 22);
    }

    /** Build sample JSON */
    private static String testExampleAsJson() {
        return "{\n"
            + "  \"date\": \"1970-01-02\",\n"
            + "  \"eventCount\": 1,\n"
            + "  \"avgEventScore\": 22.0,\n"
            + "  \"histogram\": [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]\n"
            + "}";
    }

    @Test
    public void convertDailyMetricsToJson() {

        DailyMetrics dailyMetrics = testExampleAsObject();

        assertEquals(dailyMetrics.asJson(), testExampleAsJson());
    }

    @Test
    public void fromJsonRejectIncompleteJsonRecords() {

        //reuse the exampleAsJson() String but drop out a required field (the eventCount field was removed)
        String incorrectJsonObject = "{\n"
            + "  \"date\": \"1970-01-02\",\n"
            + "  \"avgEventScore\": 22.0,\n"
            + "  \"histogram\": [0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]\n"
            + "}";

        assertThrows(
            Exception.class,
            () -> DailyMetrics.parseJson(incorrectJsonObject)
        );
    }

    @Test
    /** Convert from DailyMetrics to JSON and back to DailyMetrics */
    public void TranslateFromDailyMetricsToJsonBackToDailyMetrics() {

        DailyMetrics dailyMetrics = testExampleAsObject();

        String json = dailyMetrics.asJson();

        DailyMetrics dailyMetrics_round2 = DailyMetrics.parseJson(json);

        double TOLERANCE = 0.000001;

        assertEquals(dailyMetrics.date(), dailyMetrics_round2.date());
        assertEquals(dailyMetrics.eventCount(), dailyMetrics_round2.eventCount());
        assertEquals(dailyMetrics.avgScore(), dailyMetrics_round2.avgScore(), TOLERANCE);
        assertTrue(Arrays.equals(dailyMetrics.histogram(),
            dailyMetrics_round2.histogram()));
    }

    @Test
    /** Convert from JSON to DailyMetrics and back to JSON */
    public void TranslateFromJsonToDailyMetricsBackToJson() {

        String jsonObject = testExampleAsJson();

        DailyMetrics dailyMetrics = DailyMetrics.parseJson(jsonObject);

        String jsonObject_round2 = dailyMetrics.asJson();

        assertEquals(jsonObject, jsonObject_round2);
    }

    @Test
    public void canCombineMetrics_sameFacilities_diffDay() {

        AirborneEvent event1 = testRecord(Instant.EPOCH, 22.0);
        AirborneEvent event2 = testRecord(Instant.EPOCH.plus(Duration.ofDays(3)), 18.0);

        DailyMetrics one = new DailyMetrics(event1);
        DailyMetrics two = new DailyMetrics(event2);

        DailyMetrics combo = DailyMetrics.combine(one, two);
        assertThat(combo.date(), nullValue());
        assertThat(combo.eventCount(), is(2));
        assertThat(combo.avgScore(), is(20.0));
    }
}
