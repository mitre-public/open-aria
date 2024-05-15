
package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;

import java.time.Duration;
import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.NopHit;

import org.junit.jupiter.api.Test;

public class HighFrequencyPointRemoverTest {

    @Test
    public void testCleaningExample1() {

        Track<NopHit> testTrack = createTrackFromResource(
            HighFrequencyPointRemover.class,
            "highFrequencyPoints_example1.txt"
        );

        Duration MIN_ALLOWABLE_SPACING = Duration.ofMillis(500);

        DataCleaner<Track<NopHit>> smoother = new HighFrequencyPointRemover<>(MIN_ALLOWABLE_SPACING);

        Optional<Track<NopHit>> cleanedTrack = smoother.clean(testTrack);

        assertEquals(
            20, testTrack.size(),
            "This original track has 20 points"
        );

        assertEquals(
            13, cleanedTrack.get().size(),
            "The cleaned track should have 7 points removed (2 pairs and a set of 3)"
        );
    }

    @Test
    public void testCleaningExample2() {

        Track<NopHit> testTrack = createTrackFromResource(
            HighFrequencyPointRemover.class,
            "highFrequencyPoints_example2.txt"
        );

        Duration MIN_ALLOWABLE_SPACING = Duration.ofMillis(500);

        DataCleaner<Track<NopHit>> smoother = new HighFrequencyPointRemover<>(MIN_ALLOWABLE_SPACING);

        Optional<Track<NopHit>> cleanedTrack = smoother.clean(testTrack);

        assertEquals(
            406, testTrack.size(),
            "This original track has 406 points"
        );

        assertEquals(
            400, cleanedTrack.get().size(),
            "The cleaned track should have 6 points removed "
                + "(2 points near the beginning of the track and the very last 4 points)"
        );
    }

    @Test
    public void testCleaning_obliterate() {

        Track<NopHit> testTrack = createTrackFromResource(
            HighFrequencyPointRemover.class,
            "highFrequencyPoints_example1.txt"
        );

        Duration MIN_ALLOWABLE_SPACING = Duration.ofSeconds(10);

        DataCleaner<Track<NopHit>> smoother = new HighFrequencyPointRemover<>(MIN_ALLOWABLE_SPACING);

        Optional<Track<NopHit>> cleanedTrack = smoother.clean(testTrack);

        assertEquals(
            20, testTrack.size(),
            "This original track has 20 points"
        );

        assertFalse(
            cleanedTrack.isPresent(),
            "Only keeping points that are more than 10 seconds apart should kill the track"
        );
    }

}
