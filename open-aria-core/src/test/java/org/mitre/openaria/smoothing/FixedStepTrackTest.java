package org.mitre.openaria.smoothing;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.smoothing.FixedStepTrackMaker.fixedStepTrackFrom;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.Distance;


public class FixedStepTrackTest {

    @Test
    public void fixedStepTrack_fromVariedTrack_pointsWithinWindow() {
        Track testTrack = new SimpleTrack(Arrays.asList(
            basicPoint(EPOCH), basicPoint(EPOCH.plusSeconds(5))
        ));
        Track sampleTrack = fixedStepTrackFrom(testTrack, Duration.ofSeconds(3));

        assertEquals(testTrack.size(), sampleTrack.size());
        assertTrue(testTrack.asTimeWindow().contains(sampleTrack.asTimeWindow().start()));
        assertTrue(testTrack.asTimeWindow().contains(sampleTrack.asTimeWindow().end()));
    }

    @Test
    public void fixedStepTrack_fromVariedPoints_areFixed() {
        Track testTrack = new SimpleTrack(Arrays.asList(
            basicPoint(EPOCH), basicPoint(EPOCH.plusSeconds(5)), basicPoint(EPOCH.plusSeconds(7)), basicPoint(EPOCH.plusSeconds(13))
        ));
        Track sampleTrack = fixedStepTrackFrom(testTrack, Duration.ofSeconds(3));

        assertEquals(5, sampleTrack.size());
        assertEquals(0,
            sampleTrack.points().stream()
                .mapToLong(p -> p.time().getEpochSecond() % 3).average().getAsDouble(), 0);
    }

    @Test
    public void fixedStepTrack_withNoDuration_appliesDefault() {
        Track testTrack = new SimpleTrack(Arrays.asList(
            basicPoint(EPOCH), basicPoint(EPOCH.plusSeconds(5))
        ));
        Track sampleTrack = fixedStepTrackFrom(testTrack);

        assertEquals(EPOCH.plusSeconds(3), sampleTrack.points().last().time());
    }

    @Test
    public void checkRemainder_withNone_returnsFalse() {
        FixedStepTrackMaker trackMaker = new FixedStepTrackMaker(Duration.ofSeconds(3));
        Track testTrack = new SimpleTrack(Arrays.asList(
            basicPoint(EPOCH), basicPoint(EPOCH.plusSeconds(3))
        ));
        assertFalse(trackMaker.trackHasRemainder(testTrack));
    }

    @Test
    public void checkRemainder_withRemainder_returnsTrue() {
        FixedStepTrackMaker trackMaker = new FixedStepTrackMaker(Duration.ofSeconds(3));
        Track testTrack = new SimpleTrack(Arrays.asList(
            basicPoint(EPOCH), basicPoint(EPOCH.plusSeconds(4))
        ));
        assertTrue(trackMaker.trackHasRemainder(testTrack));
    }

    Point basicPoint(Instant time) {
        return Point.builder()
            .latLong(50.0, 50.0)
            .speed(1.0)
            .altitude(Distance.ofFeet(100))
            .time(time)
            .build();
    }
}
