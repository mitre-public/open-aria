
package org.mitre.openaria.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.parsing.nop.NopParsingUtils.parseNopTime;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.NopPoints.CenterPoint;
import org.mitre.caasd.commons.LatLong;


public class EphemeralPointTest {

    public static final String CENTER_RH_MESSAGE = "[RH],Center,ZLA_B,07-10-2016,06:16:35.000,SKW5840,CRJ2,L,4712,110,355,124,33.4922,-118.1300,465,,,,,/,,ZLA_B,,,,D0608,SAN,,IFR,,465,1396392357,LAX,,110//110,,L,1,,,{RH}";
    public static final String STARS_RH_MESSAGE = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-083.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
    public static final String AGW_RH_MESSAGE = "[RH],AGW,ABI_B,07/12/2016,19:21:08.848,N832AT,PA44,,5136,101,144,251,032.62683,-099.43983,088,5136,9.69,15.09,1,B,0,ABI,MAF,MWL,BGS,,MAF,,IFR,,39,39,TKI,,00,,S,0,,0,,94.59,96.59,{RH}";
    public static final String MEARTS_RH_MESSAGE = "[RH],MEARTS,ZUA_B,11-05-2019,15:28:06.020,UAL185,B737,L,2646,400,450,239,011.6384,141.6778,257,,67.50287,145.9169,,ZUA/1F,,ZUA_B,,,,,,,,,,,,E1430,400//400,,L,1,{RH}";

    @Test
    public void testFrom() {
        //we know the CenterPoint is good...
        EphemeralPoint point = EphemeralPoint.from(
            new CenterPoint(CENTER_RH_MESSAGE)
        );

        //make sure the information from the CenterPoint is forwarded to the EmphemeralPoint
        assertEquals("465", point.trackId());
        assertNull(point.beaconAssigned());

        assertThat("SKW5840", equalTo(point.callsign())); //confirms Hamcrest Matchers are working

        assertEquals("SKW5840", point.callsign());
        assertEquals("CRJ2", point.aircraftType());
        assertEquals("ZLA_B", point.sensor());
        assertEquals("ZLA", point.facility());
        assertEquals("4712", point.beaconActual());
        assertEquals("IFR", point.flightRules());

        assertEquals(11000.0, point.altitude().inFeet(), 0.001);
        assertEquals(33.4922, point.latLong().latitude(), 0.001);
        assertEquals(-118.1300, point.latLong().longitude(), 0.001);
        assertEquals(355, point.speedInKnots(), 0.0001);
        assertEquals(124, point.course(), 0.0001);
        assertEquals(point.time(), parseNopTime("07-10-2016", "06:16:35.000"));
    }

    static EphemeralPoint testPoint() {
        return EphemeralPoint.from(new CenterPoint(CENTER_RH_MESSAGE));
    }

    @Test
    public void testSet_speed() {
        EphemeralPoint point = testPoint();
        point.set(PointField.SPEED, 22.0);
        assertEquals(point.speedInKnots(), 22.0, 0.0001);
    }

    @Test
    public void testSet_badSpeed() {
        EphemeralPoint point = testPoint();

        assertThrows(
            IllegalArgumentException.class,
            () -> point.set(PointField.SPEED, -22.0)
        );
    }

    @Test
    public void
    testSet_latLong() {
        EphemeralPoint point = testPoint();
        point.set(PointField.LAT_LONG, LatLong.of(22.0, -11.0));
        assertEquals(point.latLong(), LatLong.of(22.0, -11.0));
    }

    @Test
    public void testSet_badLatitude() {
        EphemeralPoint point = testPoint();

        assertThrows(
            IllegalArgumentException.class, () ->
            point.set(PointField.LAT_LONG, LatLong.of(522.0, -22.0))
        );
    }

    @Test
    public void testSet_badLongitude() {
        EphemeralPoint point = testPoint();

        assertThrows(
            IllegalArgumentException.class,
            () -> point.set(PointField.LAT_LONG, LatLong.of(10.0, 522.0))
        );
    }
}
