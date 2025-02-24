
package org.mitre.openaria.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.IfrVfrStatus.IFR;
import static org.mitre.openaria.core.formats.nop.NopHit.parseSafely;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseNopTime;

import java.util.Optional;

import org.mitre.openaria.core.formats.nop.AgwRadarHit;
import org.mitre.openaria.core.formats.nop.CenterRadarHit;
import org.mitre.openaria.core.formats.nop.MeartsRadarHit;
import org.mitre.openaria.core.formats.nop.NopEncoder;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopMessage;
import org.mitre.openaria.core.formats.nop.NopMessageType;
import org.mitre.openaria.core.formats.nop.StarsRadarHit;

import org.junit.jupiter.api.Test;

public class NopPointsTest {

    public static final String CENTER_RH_MESSAGE = "[RH],Center,ZLA_B,07-10-2016,06:16:35.000,SKW5840,CRJ2,L,4712,110,355,124,33.4922,-118.1300,465,,,,,/,,ZLA_B,,,,D0608,SAN,,IFR,,465,1396392357,LAX,,110//110,,L,1,,,{RH}";
    public static final String STARS_RH_MESSAGE = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-083.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
    public static final String AGW_RH_MESSAGE = "[RH],AGW,ABI_B,07/12/2016,19:21:08.848,N832AT,PA44,,5136,101,144,251,032.62683,-099.43983,088,5136,9.69,15.09,1,B,0,ABI,MAF,MWL,BGS,,MAF,,IFR,,39,39,TKI,,00,,S,0,,0,,94.59,96.59,{RH}";
    public static final String MEARTS_RH_MESSAGE = "[RH],MEARTS,ZUA_B,11-05-2019,15:28:06.020,UAL185,B737,L,2646,400,450,239,011.6384,141.6778,257,,67.50287,145.9169,,ZUA/1F,,ZUA_B,,,,,,,,,,,,E1430,400//400,,L,1,{RH}";

    @Test
    public void testConstructors() {

        assertDoesNotThrow(() -> {
            NopHit cp1 = new NopHit(CENTER_RH_MESSAGE);
            NopHit sp1 = new NopHit(STARS_RH_MESSAGE);
            NopHit ap1 = new NopHit(AGW_RH_MESSAGE);
            NopHit mp1 = new NopHit(MEARTS_RH_MESSAGE);

            NopHit cp2 = new NopHit((CenterRadarHit) NopMessageType.parse(CENTER_RH_MESSAGE));
            NopHit sp2 = new NopHit((StarsRadarHit) NopMessageType.parse(STARS_RH_MESSAGE));
            NopHit ap2 = new NopHit((AgwRadarHit) NopMessageType.parse(AGW_RH_MESSAGE));
            NopHit mp2 = new NopHit((MeartsRadarHit) NopMessageType.parse(MEARTS_RH_MESSAGE));
        });
    }

    @Test
    public void testConstructor_nullInput() {

        String s = null;

        try {
            NopHit cp1 = new NopHit(s);
            fail("Should not work because input is null");
        } catch (NullPointerException npe) {
            assertTrue(npe.getMessage().contains("The input String cannot be null"));
        }
    }

    @Test
    public void testFactorMethod_nullInput() {
        String nullString = null;

        try {
            Point<NopHit> p = NopHit.from(nullString);
            fail("Should not work because input is null");
        } catch (NullPointerException npe) {
            assertTrue(npe.getMessage().contains("Cannot parse"));
        }

        NopMessage nullMessage = null;

        try {
            Point<NopHit> p = NopHit.from(nullMessage);
            fail("Should not work because input is null");
        } catch (NullPointerException npe) {
            assertTrue(npe.getMessage().contains("Cannot create a NopPoint from a null NopMessage"));
        }
    }

    @Test
    public void testFactoryMethod_nonRhMessage() {

        String flightPlanMessage = "[FP],Center,ZLA_B,07-10-2016,06:16:24.000,UAL455,A320,L,3356,330,E,0621,KSFO,CZQ084060,460,KLAS,,065,KSFO./.MOD148032..FUZZY.SUNST3.KLAS/0703,131667051,{FP}";

        try {
            Point<NopHit> p = NopHit.from(flightPlanMessage);
            fail("Should not work because the flightPlanMessage is not an RH message");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("Cannot create a NopPoint from a FLIGHT_PLAN"));
        }
    }

    @Test
    public void testUsage() {

        NopHit center = new NopHit(CENTER_RH_MESSAGE);
        NopHit stars = new NopHit(STARS_RH_MESSAGE);
        NopHit agw = new NopHit(AGW_RH_MESSAGE);
        NopHit mearts = new NopHit(MEARTS_RH_MESSAGE);

        assertEquals("465", center.trackId());
        assertEquals("1519", stars.trackId());
        assertEquals("088", agw.trackId());
        assertEquals("257", mearts.trackId());

        assertNull(center.beaconAssigned());
        assertEquals("1311", stars.beaconAssigned());
        assertEquals("5136", agw.beaconAssigned());
        assertNull(mearts.beaconAssigned());

        assertEquals("SKW5840", center.callsign());
        assertEquals("CRJ2", center.aircraftType());
        assertEquals("ZLA_B", center.sourceDetails().sensor());
        assertEquals("ZLA", center.sourceDetails().facility());
        assertEquals("4712", center.beaconActual());
        assertEquals("IFR", center.flightRules());

        assertEquals(11000.0, center.altitude().inFeet(), 0.001);
        assertEquals(15900.0, stars.altitude().inFeet(), 0.001);
        assertEquals(10100.0, agw.altitude().inFeet(), 0.001);
        assertEquals(40000.0, mearts.altitude().inFeet(), 0.001);

        assertEquals(33.4922, center.latLong().latitude(), 0.001);
        assertEquals(-118.1300, center.latLong().longitude(), 0.001);

        assertEquals(355, center.speedInKnots(), 0.0001);
        assertEquals(124, center.course(), 0.0001);

        assertEquals(
            NopMessageType.parse(AGW_RH_MESSAGE).rawMessage(),
            agw.rawMessage().rawMessage()
        );

        Point<NopHit> nopPoint = NopHit.from(
            "[RH],AGW,RDG,09/20/2017,17:28:02.096,,,,2525,000,425,252,040.49450,-075.76505,110,,10.66,5.09,,,,RDG,,,,,???,,,,,4221,???,,00,,,1,,0,,90.31,88.64,{RH}"
        );
        assertThat(nopPoint.time(), is(parseNopTime("09/20/2017", "17:28:02.096")));
    }

    @Test
    public void testAccessToNopRhMessageNop() {
        /*
         * Test verifies that each NopPoint can provide access to the correct type of
         * NopRadarHit
         */
        NopHit center = new NopHit(CENTER_RH_MESSAGE);
        NopHit stars = new NopHit(STARS_RH_MESSAGE);
        NopHit agw = new NopHit(AGW_RH_MESSAGE);
        NopHit mearts = new NopHit(MEARTS_RH_MESSAGE);

        assertTrue(center.rawMessage() instanceof CenterRadarHit);
        assertTrue(stars.rawMessage() instanceof StarsRadarHit);
        assertTrue(agw.rawMessage() instanceof AgwRadarHit);
        assertTrue(mearts.rawMessage() instanceof MeartsRadarHit);
    }

    @Test
    public void testAsNop() {
        /*
         * This test verifies that the "asNop" method returns the original String that was used to
         * produce the NopPoint in the first place.
         */
        Point<NopHit> center = NopHit.from(CENTER_RH_MESSAGE);
        Point<NopHit> stars = NopHit.from(STARS_RH_MESSAGE);
        Point<NopHit> agw = NopHit.from(AGW_RH_MESSAGE);
        Point<NopHit> mearts = NopHit.from(MEARTS_RH_MESSAGE);

        NopEncoder nopEncoder = new NopEncoder();

        assertEquals(CENTER_RH_MESSAGE, nopEncoder.asRawNop(center));
        assertEquals(STARS_RH_MESSAGE, nopEncoder.asRawNop(stars));
        assertEquals(AGW_RH_MESSAGE, nopEncoder.asRawNop(agw));
        assertEquals(MEARTS_RH_MESSAGE, nopEncoder.asRawNop(mearts));
    }

    @Test
    public void repeatedlyCallingTheSameAccessorMethodGivesTheSameResult() {

        /*
         * Here we confirm that repeatedly getting the assigned beaconcode from each point ALWAYS
         * gives a reference to the EXACT SAME String. Notice, we are using == and not
         * String.equals(String). This is because we want to ensure that Points built using
         * NopPoints as inputs use the flyweight pattern for all repeat Strings.
         */
        NopHit cp1 = new NopHit(CENTER_RH_MESSAGE);
        NopHit sp1 = new NopHit(STARS_RH_MESSAGE);
        NopHit ap1 = new NopHit(AGW_RH_MESSAGE);
        NopHit mp1 = new NopHit(MEARTS_RH_MESSAGE);

        //these assertions can be false if the beaconAssigned is lazily parsed out of a larger String
        assertTrue(cp1.beaconAssigned() == cp1.beaconAssigned());
        assertTrue(sp1.beaconAssigned() == sp1.beaconAssigned());
        assertTrue(ap1.beaconAssigned() == ap1.beaconAssigned());
        assertTrue(mp1.beaconAssigned() == mp1.beaconAssigned());
    }

    /*
     * This is a somewhat rare CENTER point in which the "flightRules" field is OTP. Normally the
     * "flightRules" field is "IFR" or "VFR".
     */
    public static final String CENTER_RH_WITH_OTP = "[RH],Center,ZSE,10-18-2016,23:59:48.000,CFS779,C208,G,4605,056,175,054,46.6208,-119.6453,728,,,,,/,,ZSE,,,,D2345,GEG,,OTP,,728,1179828611,YKM,,056//UNK,,S,1,,,{RH}";

    @Test
    public void centerPointWhereFlightRulesEqualOtp() {
        NopHit point = new NopHit(CENTER_RH_WITH_OTP);
        assertThat(point.flightRules(), is("OTP"));

        assertThat(IfrVfrStatus.from(point.flightRules()), is(IFR));
    }

    public static final String MEARTS_RH_NO_TRACKID = "[RH],MEARTS,ZUA_B,11-05-2019,15:28:06.020,UAL185,B737,L,2646,400,450,239,011.6384,141.6778,,,67.50287,145.9169,,ZUA/1F,,ZUA_B,,,,,,,,,,,,E1430,400//400,,L,1,{RH}";

    @Test
    public void meartsPointWithoutTrackId_doesNotThrowException() {

        Point<NopHit> p = NopHit.from(MEARTS_RH_NO_TRACKID);
        assertThat(p.hasTrackId(), is(false));
        assertThat(p.trackIdIsMissing(), is(true));
        assertThat(p.trackId(), nullValue());
    }

    @Test
    public void parseSafely_goodResultFromGoodInput() {
        Optional<Point<NopHit>> starsOpt = parseSafely(STARS_RH_MESSAGE);
        assertThat(starsOpt.isPresent(), is(true));

        Optional<Point<NopHit>> centerOpt = parseSafely(CENTER_RH_MESSAGE);
        assertThat(centerOpt.isPresent(), is(true));

        Optional<Point<NopHit>> agwOpt = parseSafely(AGW_RH_MESSAGE);
        assertThat(agwOpt.isPresent(), is(true));

        Optional<Point<NopHit>> meartOpt = parseSafely(MEARTS_RH_MESSAGE);
        assertThat(meartOpt.isPresent(), is(true));
    }

    @Test
    public void parseSafely_returnEmptyOptionalFromBadInput() {
        Optional<Point<NopHit>> optFromNull = parseSafely(null);
        assertThat(optFromNull.isPresent(), is(false));

        //longitude of -183 is illegal
        String badLongitude = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-183.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
        Optional<Point<NopHit>> optFromBadLong = parseSafely(badLongitude);
        assertThat(optFromBadLong.isPresent(), is(false));

        //latitude of -91.27 is illegal
        String badLatitude = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,091.27719,-83.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
        Optional<Point<NopHit>> optFromBadLat = parseSafely(badLatitude);
        assertThat(optFromBadLat.isPresent(), is(false));

        //NOTICE -- NONE OF THESE THROW EXCEPTION -- THEY PROVIDE EMPTY OPTIONALS
    }
}
