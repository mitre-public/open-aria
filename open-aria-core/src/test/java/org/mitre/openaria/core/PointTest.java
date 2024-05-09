
package org.mitre.openaria.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


public class PointTest {

    @Test
    public void testHasValidCallsign_false() {
        String rawNop = "[RH],STARS,D21_B,03/24/2018,15:09:14.157,,,,1200,015,059,062,042.20658,-083.77467,2643,0000,-17.6355,-0.3504,,,,D21,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";
        Point point = NopPoint.from(rawNop);

        assertFalse(point.hasValidCallsign());
        assertTrue(point.callsignIsMissing());
    }

    @Test
    public void testHasValidCallsign_true() {
        String rawNop = "[RH],STARS,D21_B,03/24/2018,14:42:00.130,N518SP,C172,,5256,032,110,186,042.92704,-083.70974,3472,5256,-14.5730,42.8527,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";
        Point point = NopPoint.from(rawNop);

        assertTrue(point.hasValidCallsign());
        assertFalse(point.callsignIsMissing());
    }

    @Test
    public void testBeaconAsInt() {
        String rawNop = "[RH],STARS,D21_B,03/24/2018,14:42:00.130,N518SP,C172,,5256,032,110,186,042.92704,-083.70974,3472,5256,-14.5730,42.8527,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";
        Point<NopPoint> point = NopPoint.from(rawNop);

        assertEquals(5256, point.rawData().beaconActualAsInt());
    }

    @Test
    public void testHasValidBeacon() {
        String rawNop = "[RH],STARS,D21_B,03/24/2018,14:42:00.130,N518SP,C172,,5256,032,110,186,042.92704,-083.70974,3472,5256,-14.5730,42.8527,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";
        Point<NopPoint> point = NopPoint.from(rawNop);

        assertTrue(point.rawData().hasValidBeaconActual());
    }


    @Test
    public void test_hasTrackId() {
        String rawNopNoTrackId = "[RH],STARS,D21_B,03/24/2018,15:09:14.157,,,,1200,015,059,062,042.20658,-083.77467,,0000,-17.6355,-0.3504,,,,D21,,,,,,ACT,,,00000,,,,,,,1,,0,{RH}";
        Point p = NopPoint.from(rawNopNoTrackId);

        assertThat(p.hasTrackId(), is(false));
        assertThat(p.trackIdIsMissing(), is(true));
        assertThat(p.trackId(), is(""));

        String rawNopWithTrackId = "[RH],STARS,D21_B,03/24/2018,15:09:14.157,,,,1200,015,059,062,042.20658,-083.77467,1234,0000,-17.6355,-0.3504,,,,D21,,,,,,ACT,,,00000,,,,,,,1,,0,{RH}";
        Point p1 = NopPoint.from(rawNopWithTrackId);

        assertThat(p1.hasTrackId(), is(true));
        assertThat(p1.trackIdIsMissing(), is(false));
        assertThat(p1.trackId(), is("1234"));
    }
}
