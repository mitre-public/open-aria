
package org.mitre.openaria.core;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;

import org.junit.jupiter.api.Test;

public class TrackPairCleanerTest {

    //a simple test cleaner
    static class SizeBasedCleaner<T> implements DataCleaner<Track<T>> {

        final int requiredSize;

        SizeBasedCleaner(int requiredSize) {
            this.requiredSize = requiredSize;
        }

        @Override
        public Optional<Track<T>> clean(Track<T> track) {
            return (track.size() >= requiredSize)
                ? Optional.of(track)
                : Optional.empty();
        }
    }

    @Test
    public void rejectingOneTrackProducesEmptyOptional() {

        Track<String> track1 = testTrack(15);
        Track<String> track2 = testTrack(5);

        TrackPairCleaner instance = new TrackPairCleaner(new SizeBasedCleaner(10));

        Optional<TrackPair> result = instance.clean(TrackPair.of(track1, track2));

        assertFalse(result.isPresent());
    }

    @Test
    public void rejectingNoTracksProducesUsableOptional() {

        Track<String> track1 = testTrack(15);
        Track<String> track2 = testTrack(5);

        TrackPairCleaner instance = new TrackPairCleaner(new SizeBasedCleaner(2));

        Optional<TrackPair> result = instance.clean(TrackPair.of(track1, track2));

        assertTrue(result.isPresent());
        assertThat(result.get().track1(), is(track1));
        assertThat(result.get().track2(), is(track2));
    }

    private Track<String> testTrack(int numPoints) {

        ArrayList<Point<String>> points = newArrayList();

        LatLong startPoint = LatLong.of(0.0, 0.0);

        for (int i = 0; i < numPoints; i++) {
            Point<String> newPoint = Point.<String>builder()
                .time(EPOCH.plusSeconds(i))
                .latLong(startPoint.projectOut(90.0, (double) i))
                .build();
            points.add(newPoint);
        }
        return Track.of(points);
    }

}
