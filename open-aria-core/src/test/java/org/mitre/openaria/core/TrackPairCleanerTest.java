
package org.mitre.openaria.core;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;

public class TrackPairCleanerTest {

    //a simple test cleaner
    class SizeBasedCleaner implements DataCleaner<Track> {

        final int requiredSize;

        SizeBasedCleaner(int requiredSize) {
            this.requiredSize = requiredSize;
        }

        @Override
        public Optional<Track> clean(Track track) {
            return (track.size() >= requiredSize)
                ? Optional.of(track)
                : Optional.empty();
        }
    }

    @Test
    public void rejectingOneTrackProducesEmptyOptional() {

        Track track1 = testTrack(15);
        Track track2 = testTrack(5);

        TrackPairCleaner instance = new TrackPairCleaner(new SizeBasedCleaner(10));

        Optional<TrackPair> result = instance.clean(TrackPair.of(track1, track2));

        assertFalse(result.isPresent());
    }

    @Test
    public void rejectingNoTracksProducesUsableOptional() {

        Track track1 = testTrack(15);
        Track track2 = testTrack(5);

        TrackPairCleaner instance = new TrackPairCleaner(new SizeBasedCleaner(2));

        Optional<TrackPair> result = instance.clean(TrackPair.of(track1, track2));

        assertTrue(result.isPresent());
        assertTrue(result.get().track1() == track1);
        assertTrue(result.get().track2() == track2);
    }

    private Track testTrack(int numPoints) {

        ArrayList<Point> points = newArrayList();

        LatLong startPoint = LatLong.of(0.0, 0.0);

        for (int i = 0; i < numPoints; i++) {
            Point newPoint = Point.builder()
                .time(EPOCH.plusSeconds(i))
                .latLong(startPoint.projectOut(90.0, (double) i))
                .build();
            points.add(newPoint);
        }
        return new SimpleTrack(points);
    }

}
