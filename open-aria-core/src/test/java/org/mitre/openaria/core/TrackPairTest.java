
package org.mitre.openaria.core;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.TrackPairs.overlapInTime;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;


public class TrackPairTest {

    final static Point<NopPoint> P1 = NopPoint.from("[RH],STARS,ZOB,06/30/2017,16:40:17.000,N63886,PA27,,1060,70,150,65,39.09000,-79.52830,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
    final static Point<NopPoint>  P2 = NopPoint.from("[RH],STARS,ZOB,06/30/2017,16:40:29.000,N63886,PA27,,1060,70,150,66,39.09280,-79.51780,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
    final static Point<NopPoint>  P3 = NopPoint.from("[RH],STARS,ZOB,06/30/2017,16:40:42.000,N63886,PA27,,1060,71,151,68,39.09580,-79.50610,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
    final static Point<NopPoint>  P4 = NopPoint.from("[RH],STARS,ZOB,06/30/2017,16:40:54.000,N63886,PA27,,1060,71,151,68,39.09830,-79.49670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
    final static Point<NopPoint>  P5 = NopPoint.from("[RH],STARS,ZOB,06/30/2017,16:41:07.000,N63886,PA27,,1060,73,151,68,39.10140,-79.48670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
    final static Point<NopPoint>  P6 = NopPoint.from("[RH],STARS,ZOB,06/30/2017,16:41:19.000,N63886,PA27,,1060,74,151,68,39.10530,-79.47720,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");

    final static Track A_TRACK = Track.of(newArrayList(P1, P2, P3, P4, P5, P6));

    @Test
    public void testConstructor() {
        Track track1 = Track.of(newArrayList(P1, P2, P3));
        Track track2 = Track.of(newArrayList(P4, P5, P6));

        TrackPair pair = new TrackPair(track1, track2);
        assertEquals(pair.track1(), track1);
        assertEquals(pair.track2(), track2);
    }

    @Test
    public void testOf() {

        Track track1 = Track.of(newArrayList(P1, P2, P3));
        Track track2 = Track.of(newArrayList(P4, P5, P6));

        TrackPair pair = TrackPair.of(track1, track2);
        assertEquals(pair.track1(), track1);
        assertEquals(pair.track2(), track2);
    }

    @Test
    public void testOf_nullInput_1st() {

        assertThrows(
            NullPointerException.class,
            () -> TrackPair.of(null, A_TRACK)
        );
    }

    @Test
    public void testOf_nullInput_2nd() {

        assertThrows(
            NullPointerException.class,
            () -> TrackPair.of(A_TRACK, null)
        );
    }

    @Test
    public void testOverlapInTime() {

        Track fullTrack = Track.of(newArrayList(P1, P2, P3, P4, P5, P6));
        Track earlyTrack = Track.of(newArrayList(P1, P2, P3));
        Track endTrack = Track.of(newArrayList(P4, P5, P6));
        Track endTrack_2 = Track.of(newArrayList(P3, P4, P5, P6));

        assertTrue(overlapInTime(fullTrack, earlyTrack));
        assertTrue(overlapInTime(earlyTrack, fullTrack));

        assertTrue(overlapInTime(fullTrack, endTrack));
        assertTrue(overlapInTime(endTrack, fullTrack));

        assertTrue(overlapInTime(earlyTrack, endTrack_2));
        assertTrue(overlapInTime(endTrack_2, earlyTrack));

        assertFalse(overlapInTime(earlyTrack, endTrack));
        assertFalse(overlapInTime(endTrack, earlyTrack));
    }

    @Test
    public void testFromCollection_happyPath() {

        Track earlyTrack = Track.of(newArrayList(P1, P2, P3));
        Track fullTrack = Track.of(newArrayList(P1, P2, P3, P4, P5, P6));

        ArrayList<Track> tracks = newArrayList(fullTrack, earlyTrack);

        TrackPair pair = TrackPair.from(tracks);

        assertNotEquals(pair.track1(), pair.track2());
        assertTrue(pair.track1() == earlyTrack || pair.track2() == earlyTrack);
        assertTrue(pair.track1() == fullTrack || pair.track2() == fullTrack);
    }

    @Test
    public void testFromCollection_tooFewTracks() {

        Track oneTrack = Track.of(newArrayList(P1, P2, P3));

        ArrayList<Track> tracks = newArrayList(oneTrack);

        assertThrows(
            IllegalArgumentException.class,
            () -> TrackPair.from(tracks),
            "This should fail because there aren't enough tracks"
        );
    }

    @Test
    public void testFromCollection_tooManyTracks() {

        Track track1 = Track.of(newArrayList(P1, P2, P3));
        Track track2 = Track.of(newArrayList(P1, P2, P3, P4, P5, P6));
        Track track3 = Track.of(newArrayList(P3, P4, P5, P6));

        ArrayList<Track> tracks = newArrayList(track1, track2, track3);

        assertThrows(
            IllegalArgumentException.class,
            () -> TrackPair.from(tracks),
            "This should fail because there are too many tracks"
        );
    }
}
