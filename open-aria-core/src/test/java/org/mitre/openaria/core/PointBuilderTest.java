
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.openaria.core.temp.Extras.AircraftDetails;
import org.mitre.openaria.core.temp.Extras.SourceDetails;

import org.junit.jupiter.api.Test;

public class PointBuilderTest {

    @Test
    public void testCallsign() {

        AircraftDetails acDetails = new AircraftDetails("call", "acType");

        CommonPoint p1 = (new PointBuilder()).acDetails(acDetails).build();
        CommonPoint p2 = (new PointBuilder()).acDetails(null).build();

        assertTrue(p1.acDetails().callsign().equals("call"));
        assertTrue(isNull(p2.acDetails()));
    }

    @Test
    public void testSensor() {

        SourceDetails sd = new SourceDetails("aSensor", "aFacility");

        CommonPoint p1 = (new PointBuilder()).sourceDetails(sd).build();
        CommonPoint p2 = (new PointBuilder()).sourceDetails(null).build();

        assertThat(p1.sourceDetails(), is(sd));
        assertThat(p2.sourceDetails(), nullValue());
    }

//    @Test
//    public void testFacility() {
//
//        String stringValue = "aString";
//
//        Point p1 = (new PointBuilder()).facility(stringValue).build();
//        Point p2 = (new PointBuilder()).facility(null).build();
//
//        assertTrue(p1.facility().equals(stringValue));
//        assertTrue(isNull(p2.facility()));
//    }

    @Test
    public void testBeaconActual() {

        String stringValue = "aString";

        Point p1 = (new PointBuilder()).beaconActual(stringValue).build();
        Point p2 = (new PointBuilder()).beaconActual(null).build();

        assertTrue(p1.beaconActual().equals(stringValue));
        assertTrue(isNull(p2.beaconActual()));
    }

    @Test
    public void testLatLong() {
        LatLong latLong = LatLong.of(-5.0, -2.34);
        Point p = Point.builder().latLong(latLong).build();
        assertEquals(p.latLong(), latLong);
    }

    @Test
    public void testLatLong_Double_Double() {
        Point p = Point.builder().latLong(1.23, 4.56).time(EPOCH).build();
        assertEquals(p.latLong(), LatLong.of(1.23, 4.56));
    }

    @Test
    public void testLatLong_Double_Double_nullLat() {
        assertThrows(
            NullPointerException.class,
            () -> Point.builder().latLong(null, 4.56)
        );
    }

    @Test
    public void testLatLong_Double_Double_nullLong() {
        assertThrows(
            NullPointerException.class,
            () -> Point.builder().latLong(1.23, null)
        );
    }

    @Test
    public void testAltitudeInFeet() {

        Double doubleValue = 5.0;

        Point p1 = (new PointBuilder()).altitude(Distance.ofFeet(doubleValue)).build();
        Point p2 = (new PointBuilder()).altitude(null).build();

        assertEquals(p1.altitude().inFeet(), doubleValue, 0.001);
        assertTrue(isNull(p2.altitude()));
    }

    @Test
    public void testCourseInDegrees() {

        Double doubleValue = 5.0;

        Point p1 = (new PointBuilder()).courseInDegrees(doubleValue).build();
        Point p2 = (new PointBuilder()).courseInDegrees(null).build();

        assertTrue(p1.course().equals(doubleValue));
        assertTrue(isNull(p2.course()));
    }

    @Test
    public void testSpeed() {

        Double doubleValue = 5.0;

        Point p1 = (new PointBuilder()).speed(doubleValue).build();
        Point p2 = (new PointBuilder()).speed(null).build();

        assertTrue(p1.speedInKnots().equals(doubleValue));
        assertTrue(isNull(p2.speedInKnots()));
    }

    @Test
    public void testSpeed_badValue() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new PointBuilder().speed(-1.0),
            "negative speeds should throw exceptions"
        );
    }

    @Test
    public void testTime() {

        Instant time = Instant.EPOCH;

        Point p1 = (new PointBuilder()).time(time).build();
        Point p2 = (new PointBuilder()).time(null).build();

        assertTrue(p1.time().equals(time));
        assertTrue(isNull(p2.time()));
    }

    private CommonPoint getTestPoint() {

        return (new PointBuilder())
            .acDetails(new AircraftDetails("callsign", "acType"))
            .sourceDetails(new SourceDetails("sensor", "facility"))
            .time(Instant.now())
            .build();
    }

    @Test
    public void testCopier() {

        Point testPoint = getTestPoint();
        Point copiedPoint = (new PointBuilder(testPoint)).build();

        Map<PointField, Object> testValues = Points.toMap(testPoint);
        Map<PointField, Object> copiedValues = Points.toMap(copiedPoint);

        for (Map.Entry<PointField, Object> entry : testValues.entrySet()) {
            assertTrue(entry.getValue() == copiedValues.get(entry.getKey()));
        }
    }



//    @Test
//    public void testButSensor() {
//
//        Point testPoint = getTestPoint();
//
//        Point p1 = (new PointBuilder(testPoint)).butSensor("newValue").build();
//
//        assertTrue(p1.sensor().equals("newValue"));
//    }

    @Test
    public void test_butLatLong() {
        Point p = Point.builder().latLong(0.0, 0.0).time(EPOCH).build();
        Point p2 = Point.builder(p).butLatLong(1.0, 1.0).build();

        assertEquals(p2.latLong(), LatLong.of(1.0, 1.0));
        assertEquals(p2.time(), EPOCH);
    }
}
