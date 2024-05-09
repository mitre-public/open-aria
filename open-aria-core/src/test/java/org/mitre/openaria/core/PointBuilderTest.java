
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

import org.junit.jupiter.api.Test;

public class PointBuilderTest {

    /* Declare a simple Pojo that holds data specific to a particular type of raw data. */
    record PojoWithCallsign(String callsign) {};


    @Test
    public void testPullingValueFromRawData() {

        PojoWithCallsign pwcs = new PojoWithCallsign("someCallsign");

        Point<PojoWithCallsign> p1 = new PointBuilder<PojoWithCallsign>().rawData(pwcs)
            .time(EPOCH).latLong(0.0, 0.0).build();

        Point<PojoWithCallsign> p2 = (new PointBuilder<PojoWithCallsign>()).rawData(null)
            .time(EPOCH).latLong(0.0, 0.0).build();

        assertThat(p1.rawData().callsign, is("someCallsign"));
        assertThat(p2.rawData(), nullValue());
    }

    @Test
    public void testLatLong() {
        LatLong latLong = LatLong.of(-5.0, -2.34);
        Point p = Point.builder().latLong(latLong).time(EPOCH).build();
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

        Point p1 = (new PointBuilder()).time(EPOCH).latLong(0.0, 0.0).altitude(Distance.ofFeet(doubleValue)).build();
        Point p2 = (new PointBuilder()).time(EPOCH).latLong(0.0, 0.0).altitude(null).build();

        assertEquals(p1.altitude().inFeet(), doubleValue, 0.001);
        assertThat(p1.altitude(), is(Distance.ofFeet(5.0)));
        assertThat(p2.altitude(), nullValue());
    }


//    @Test
//    public void testSpeed_badValue() {
//        assertThrows(
//            IllegalArgumentException.class,
//            () -> new PointBuilder().speed(-1.0),
//            "negative speeds should throw exceptions"
//        );
//    }

    @Test
    public void testTime() {

        Instant time = Instant.EPOCH.plusSeconds(12);

        Point<?> p1 = (new PointBuilder()).time(time)
            .latLong(0.0, 0.0)
            .build();

        assertTrue(p1.time().equals(time));
    }

    private Point getTestPoint() {

        return (new PointBuilder())
            .latLong(0.0, 1.234)
            .time(Instant.now())
            .altitude(Distance.ofFeet(5000))
            .build();
    }

    @Test
    public void testCopier() {

        Point testPoint = getTestPoint();
        Point copiedPoint = (new PointBuilder(testPoint)).build();

        assertThat(testPoint, is(copiedPoint));

    }


    @Test
    public void test_butLatLong() {
        Point p = Point.builder().latLong(0.0, 0.0).time(EPOCH).build();
        Point p2 = Point.builder(p).latLong(1.0, 1.0).build();

        assertEquals(p2.latLong(), LatLong.of(1.0, 1.0));
        assertEquals(p2.time(), EPOCH);
    }
}
