
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.NopHit;
import org.mitre.openaria.core.formats.nop.NopParsingUtils;

import org.junit.jupiter.api.Test;


/**
 * These tests ensure the TrimLowSpeedPoints and RemoveGroundData Filters works as expected.
 */
public class LowSpeedFilteringTest {

    @Test
    public void testStartingLowSpeedPointAreRemoved() {

        Point<NopHit> p1 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:17.000,N63886,PA27,,1060,70,0,65,39.09000,-79.52830,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p2 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:29.000,N63886,PA27,,1060,70,0,66,39.09280,-79.51780,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p3 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:42.000,N63886,PA27,,1060,71,151,68,39.09580,-79.50610,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p4 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:54.000,N63886,PA27,,1060,71,151,68,39.09830,-79.49670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p5 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:07.000,N63886,PA27,,1060,73,151,68,39.10140,-79.48670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p6 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:19.000,N63886,PA27,,1060,74,151,68,39.10530,-79.47720,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");

        Track fullTrack = Track.of(newArrayList(p1, p2, p3, p4, p5, p6));

        //should remove p1 and p2
        Optional<Track> cleanedTrack = (new TrimLowSpeedPoints(50, 1)).clean(fullTrack);

        assertTrue(cleanedTrack.isPresent());
        assertTrue(cleanedTrack.get().size() == 4);
        assertTrue(cleanedTrack.get().points().first().time().equals(
            NopParsingUtils.parseNopTime("06/30/2017", "16:40:42.000")
        ));
    }

    @Test
    public void testTrailingLowSpeedPointAreRemoved() {

        Point<NopHit> p1 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:17.000,N63886,PA27,,1060,70,151,65,39.09000,-79.52830,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p2 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:29.000,N63886,PA27,,1060,70,151,66,39.09280,-79.51780,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p3 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:42.000,N63886,PA27,,1060,71,151,68,39.09580,-79.50610,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p4 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:54.000,N63886,PA27,,1060,71,151,68,39.09830,-79.49670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p5 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:07.000,N63886,PA27,,1060,73,0,68,39.10140,-79.48670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p6 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:19.000,N63886,PA27,,1060,74,0,68,39.10530,-79.47720,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");

        Track fullTrack = Track.of(newArrayList(p1, p2, p3, p4, p5, p6));

        //should remove p5 and p6
        Optional<Track> cleanedTrack = (new TrimLowSpeedPoints(50, 1)).clean(fullTrack);

        assertTrue(cleanedTrack.isPresent());
        assertTrue(cleanedTrack.get().size() == 4);
        assertTrue(cleanedTrack.get().points().last().time().equals(
            NopParsingUtils.parseNopTime("06/30/2017", "16:40:54.000")
        ));
    }

    @Test
    public void testSmallTracksAreRemoved() {

        Point<NopHit> p1 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:17.000,N63886,PA27,,1060,70,0,65,39.09000,-79.52830,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p2 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:29.000,N63886,PA27,,1060,70,151,66,39.09280,-79.51780,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p3 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:42.000,N63886,PA27,,1060,71,151,68,39.09580,-79.50610,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p4 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:40:54.000,N63886,PA27,,1060,71,151,68,39.09830,-79.49670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p5 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:07.000,N63886,PA27,,1060,73,151,68,39.10140,-79.48670,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");
        Point<NopHit> p6 = NopHit.from("[RH],STARS,ZOB,06/30/2017,16:41:19.000,N63886,PA27,,1060,74,0,68,39.10530,-79.47720,755,,,,,,,ZOB_B,,,,,,,IFR,,,,,,,,,,,,{RH}");

        Track fullTrack = Track.of(newArrayList(p1, p2, p3, p4, p5, p6));

        //should remove p1 and p6
        Optional<Track> cleanedTrack = new TrimLowSpeedPoints(50, 5).clean(fullTrack);

        assertFalse(cleanedTrack.isPresent());
    }

}
