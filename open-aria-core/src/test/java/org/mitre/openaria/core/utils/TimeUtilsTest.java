package org.mitre.openaria.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;


public class TimeUtilsTest {

    @Test
    public void testDurationFormatting_1() {

        //13 hours, 22 minutes, and 15 seconds
        long numSeconds = 13 * 3600 + 22 * 60 + 15;

        Duration dur = Duration.ofSeconds(numSeconds);

        assertEquals(
            "13:22:15",
            TimeUtils.asString(dur)
        );
    }

    @Test
    public void testDurationFormatting_2() {

        //2 days, 13 hours, 22 minutes, and 15 seconds
        int SECONDS_PER_DAY = 24 * 60 * 60;
        long numSeconds = 2 * SECONDS_PER_DAY + 13 * 3600 + 22 * 60 + 15;

        Duration dur = Duration.ofSeconds(numSeconds);

        assertEquals(
            "2 days, 13:22:15",
            TimeUtils.asString(dur)
        );
    }

    @Test
    public void testUtcDateAsString() {

        assertEquals(
            "1970-01-01",
            TimeUtils.utcDateAsString(Instant.EPOCH)
        );

        assertEquals(
            "1970-01-03",
            TimeUtils.utcDateAsString(Instant.EPOCH.plus(2, ChronoUnit.DAYS))
        );
    }

}