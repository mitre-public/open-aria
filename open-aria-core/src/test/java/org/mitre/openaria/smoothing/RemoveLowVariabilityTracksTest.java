
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;

import java.util.Optional;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;


public class RemoveLowVariabilityTracksTest {

    @Test
    public void testFilteringOnShortTrack() {

        HasLowVariability predicate = new HasLowVariability();

        assertFalse(
            predicate.test(trackThatsTooShort()),
            "This track is too short -- so it cant \"HaveLowVariance\""
        );

        RemoveLowVariabilityTracks filter = new RemoveLowVariabilityTracks(predicate);

        Optional<Track> result = filter.clean(trackThatsTooShort());

        assertTrue(
            result.isPresent(),
            "If the track wasn't deemed to have low variance it should make it through the filter"
        );
    }

    private static Track trackThatsTooShort() {
        //this track is bad because it is too short for the "low variance predicate" to apply

        Point singlePoint = Point.builder()
            .latLong(50.0, 50.0)
            .time(EPOCH)
            .build();

        return Track.of(newArrayList(singlePoint));
    }

    @Test
    public void testFilteringOnRealData() {

        Track trackFromBadData = erroneousTrackFromRadarMirage();

        HasLowVariability predicate = new HasLowVariability();

        assertTrue(predicate.test(trackFromBadData));

        RemoveLowVariabilityTracks filter = new RemoveLowVariabilityTracks(predicate);

        Optional<Track> result = filter.clean(trackFromBadData);

        assertFalse(result.isPresent(), "This track should not make it through the filter");
    }

    /*
     * The REAL data in this track barely moves, thus the track should be tagged as "having low
     * variability"
     */
    public static Track erroneousTrackFromRadarMirage() {

        return createTrackFromResource(
            RemoveLowVariabilityTracks.class,
            "garbageStationaryTrack.txt"
        );
    }
}
