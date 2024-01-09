package org.mitre.openaria.smoothing;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Track;

public class LargeDurationOfZeroAltitudeTrackRemoverTest {

    private static Optional<Track> cleanTrack(String trackFile, int timeInSeconds) {

        Track track = createTrackFromResource(
            LargeDurationOfZeroAltitudeTrackRemover.class,
            trackFile
        );

        LargeDurationOfZeroAltitudeTrackRemover remover = new LargeDurationOfZeroAltitudeTrackRemover(ofSeconds(timeInSeconds));

        return remover.clean(track);
    }

    @Test
    public void testFilteringOutTrackWithLargeGaps() {

        Optional<Track> maybeTrack = cleanTrack(
            "largeGapOfMissingAltitudes.txt",
            60
        );

        assertFalse(maybeTrack.isPresent());
    }

    @Test
    public void testKeepingTrackWithLargeGaps() {

        Optional<Track> maybeTrack = cleanTrack(
            "largeGapOfMissingAltitudes.txt",
            600
        );

        assertTrue(maybeTrack.isPresent());
    }

    @Test
    public void testFilteringOutTrackWithManySmallGaps() {

        Optional<Track> maybeTrack = cleanTrack(
            "multipleSmallGapsOfMissingAltitudes.txt",
            60
        );

        assertFalse(maybeTrack.isPresent());
    }

    @Test
    public void testKeppingTrackWithManySmallGaps() {

        Optional<Track> maybeTrack = cleanTrack(
            "multipleSmallGapsOfMissingAltitudes.txt",
            180
        );

        assertTrue(maybeTrack.isPresent());
    }
}
