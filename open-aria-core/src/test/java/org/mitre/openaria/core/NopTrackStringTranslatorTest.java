
package org.mitre.openaria.core;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

public class NopTrackStringTranslatorTest {

    private final String nopHit1 = "[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,024,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}";
    private final String nopHit2 = "[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,034,158,271,033.63177,-084.33931,1334,5116,22.4044,27.6328,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}";

    @Test
    public void testTrackToNopStringTranslation() {
        NopTrackStringTranslator translator = new NopTrackStringTranslator();

        Track trackWithNopPoints = trackWithNopPoints();
        String nopTrackAsString = translator.to(trackWithNopPoints);

        assertEquals(nopHit1 + "\n" + nopHit2 + "\n", nopTrackAsString);
    }

    private Track trackWithNopPoints() {
        Point p1 = NopPoint.from(nopHit1);
        Point p2 = NopPoint.from(nopHit2);

        List<Point> points = Lists.newArrayList(p1, p2);
        return new SimpleTrack(points);
    }

    @Test
    public void testTrackFromNopStringTranslation() {
        NopTrackStringTranslator translator = new NopTrackStringTranslator();

        String nopTrackString = nopHit1 + "\n" + nopHit2 + "\n";
        Track track = translator.from(nopTrackString);

        assertEquals(NopPoints.StarsPoint.class, track.points().first().getClass());
        assertEquals("JIA5545", track.points().first().callsign());
    }
}