package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.NopPoint;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;

public class RemoveTracksMatchingPredicateTest {


    private static Track testTrack() {

        Point p1 = NopPoint.from("[RH],Center,ZOB_B,07-10-2016,17:24:37.000,RPA4391,E170,L,7336,270,443,077,41.3725,-80.8414,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616763689,IND,1813,270//270,,L,1,,,{RH}");
        Point p2 = NopPoint.from("[RH],Center,ZOB_B,07-10-2016,17:24:49.000,RPA4391,E170,L,7336,270,444,077,41.3781,-80.8100,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616763984,IND,1813,270//270,,L,1,,,{RH}");
        Point p3 = NopPoint.from("[RH],Center,ZOB_B,07-10-2016,17:25:02.000,RPA4391,E170,L,7336,270,444,077,41.3839,-80.7778,809,,,,,ZOB/70,,ZOB_B,,,,E1719,JFK,,IFR,,809,616764278,IND,1813,270//270,,L,1,,,{RH}");

        return new SimpleTrack(List.of(p1, p2, p3));
    }

    @Test
    public void Filter_WhenPredicateIsTrue_RemoveTrack() {

        RemoveTracksMatchingPredicate<Track> test = new RemoveTracksMatchingPredicate<>(t -> true);
        Optional<Track> actual = test.clean(testTrack());

        assertFalse(actual.isPresent(), "Track should be removed when predicate is True");
    }

    @Test
    public void Filter_WhenPredicateIsFalse_DoNotRemoveTrack() {

        RemoveTracksMatchingPredicate<Track> test = new RemoveTracksMatchingPredicate<>(t -> false);
        Optional<Track> actual = test.clean(testTrack());

        assertTrue(actual.isPresent(), "Track should be kept when predicate is False");
    }

    @Test
    public void OnRemoval_WhenPredicateIsTrue_PipeRemovedTrack() {

        Counter counter = new Counter();
        RemoveTracksMatchingPredicate<Track> test = new RemoveTracksMatchingPredicate<>(t -> true, counter);
        Optional<Track> actual = test.clean(testTrack());

        assertFalse(actual.isPresent(), "Track should be removed when predicate is True");
        assertEquals(1, counter.count, "Removed track should be piped to consumer");
    }

    @Test
    public void OnRemoval_WhenPredicateIsFalse_DoNotPipeTrack() {

        Counter counter = new Counter();
        RemoveTracksMatchingPredicate<Track> test = new RemoveTracksMatchingPredicate<>(t -> false, counter);
        Optional<Track> actual = test.clean(testTrack());

        assertTrue(actual.isPresent(), "Track should be kept when predicate is False");
        assertEquals(0, counter.count, "Tracks that aren't removed should not reach consumer");
    }


    /**
     * Simple consumer of tracks that acts as a counter for the sake of these tests.
     */
    private static class Counter implements Consumer<Track> {

        private int count = 0;

        @Override
        public void accept(Track track) {
            count++;
        }
    }

}