
package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.IfrVfrStatus.VFR;

import org.junit.jupiter.api.Test;


public class IfrVfrAssignerTest {

    @Test
    public void testStatusOfPoint_1200_beacon() {
        String rawNop = "[RH],STARS,D21_B,03/24/2018,15:09:14.157,,,,1200,015,059,062,042.20658,-083.77467,2643,0000,-17.6355,-0.3504,,,,D21,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";
        Point<NopPoint> point = NopPoint.from(rawNop);

        assertTrue(point.rawData().hasFlightRules());
        assertFalse(point.hasValidCallsign());

        IfrVfrAssigner assigner = new IfrVfrAssigner();

        assertEquals(VFR, assigner.statusOf(point));
    }

    @Test
    public void testStatusOfPoint_goodBeaconAndGoodcallsign_vfr() {
        String rawNop = "[RH],STARS,D21_B,03/24/2018,14:42:00.130,N518SP,C172,,5256,032,110,186,042.92704,-083.70974,3472,5256,-14.5730,42.8527,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}";
        Point<NopPoint> point = NopPoint.from(rawNop);

        assertTrue(point.rawData().hasFlightRules());
        assertTrue(point.hasValidCallsign());
        assertEquals(IfrVfrStatus.from(point.rawData().flightRules()), VFR);

        IfrVfrAssigner assigner = new IfrVfrAssigner();

        assertEquals(VFR, assigner.statusOf(point));
    }

    @Test
    public void testStatusOfPoint_hasBeaconNoCallsign() {
        String rawNop = "[RH],STARS,A11_B,04/06/2018,18:17:37.168,,,,0506,005,071,272,061.21630,-149.86218,1048,0000,4.4746,2.2864,,,,A11,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";

        Point<NopPoint> point = NopPoint.from(rawNop);

        assertEquals(506, point.rawData().beaconActualAsInt());
        assertTrue(point.rawData().hasFlightRules());
        assertFalse(point.hasValidCallsign());

        IfrVfrAssigner assigner = new IfrVfrAssigner();

        assertEquals(
            VFR, assigner.statusOf(point),
            "When the callsign is missing ignore the Flight Rules tag (because it defaults to IFR)"
        );
    }

    @Test
    public void testStatusOfPoint_zeroBeaconNoCallsign() {
        //this raw source was manually edited to remove the flight rules (VFR/IFR) field
        String rawNop = "[RH],STARS,A11_B,03/31/2018,23:05:47.612,,,,0000,000,140,315,061.42058,-149.52365,2766,0000,14.2051,14.6263,,,,A11,,,,,,ACT,,,00000,,,,,,,1,,1,{RH}";

        Point<NopPoint> point = NopPoint.from(rawNop);

        assertEquals(0, point.rawData().beaconActualAsInt());
        assertFalse(point.rawData().hasFlightRules());
        assertFalse(point.hasValidCallsign());

        IfrVfrAssigner assigner = new IfrVfrAssigner();

        assertEquals(
           VFR, assigner.statusOf(point),
            "When the beacon is 0000 the aircraft should be VFR"
        );
    }

    @Test
    public void testStatusOfPoint_noFlightRules() {
        String rawNop = "[RH],AGW,BTR,10/18/2016,00:26:51.911,,,,1016,056,176,202,030.34024,-091.71407,212,,-29.21,-13.05,,,,BTR,,,,,???,,,,,6682,???,,00,,,1,,0,,53.42,68.77,{RH}";

        Point<NopPoint> point = NopPoint.from(rawNop);

        assertFalse(point.rawData().hasFlightRules());
        assertFalse(point.hasValidCallsign());

        IfrVfrAssigner assigner = new IfrVfrAssigner();

        assertEquals(
            VFR, assigner.statusOf(point),
            "No flight rules, no callsign = VFR"
        );
    }
}
