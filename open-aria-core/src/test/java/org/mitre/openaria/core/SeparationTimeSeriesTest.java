
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;


public class SeparationTimeSeriesTest {

    private Instant[] times() {
        return new Instant[]{
            EPOCH,
            EPOCH.plusSeconds(5),
            EPOCH.plusSeconds(10)
        };
    }

    //diverge vertically
    private Distance[] verticalDistances() {
        return new Distance[]{
            Distance.ofFeet(0),
            Distance.ofFeet(100),
            Distance.ofFeet(200)
        };
    }

    //converge horizontally
    private Distance[] horizontalDistances() {
        return new Distance[]{
            Distance.ofNauticalMiles(1),
            Distance.ofNauticalMiles(.5),
            Distance.ofNauticalMiles(0)
        };
    }

    @Test
    public void testConstructor() {

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertEquals(instance.timeWindow().start(), EPOCH);
        assertEquals(instance.timeWindow().end(), EPOCH.plusSeconds(10));
    }

    @Test
    public void test_verticalDistanceAt() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertEquals(Distance.ofFeet(0), instance.verticalSeparationAt(EPOCH));
        assertEquals(Distance.ofFeet(100), instance.verticalSeparationAt(EPOCH.plusSeconds(5)));
        assertEquals(Distance.ofFeet(200), instance.verticalSeparationAt(EPOCH.plusSeconds(10)));
        assertEquals(Distance.ofFeet(50), instance.verticalSeparationAt(EPOCH.plusMillis(2_500)));
        assertEquals(Distance.ofFeet(180), instance.verticalSeparationAt(EPOCH.plusSeconds(9)));
    }

    @Test
    public void test_verticalDistanceAt_beforeTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.verticalSeparationAt(EPOCH.minusSeconds(1)),
            "This should fail because this time is outside the timeWindow of these time series"
        );
    }

    @Test
    public void test_verticalDistanceAt_afterTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.verticalSeparationAt(EPOCH.plusSeconds(11)),
            "This should fail because this time is outside the timeWindow of these time series"
        );
    }

    @Test
    public void test_horizontalDistanceAt() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        double TOLERANCE = 0.000001;

        assertEquals(Distance.ofNauticalMiles(1), instance.horizontalSeparationAt(EPOCH));
        assertEquals(Distance.ofNauticalMiles(.5), instance.horizontalSeparationAt(EPOCH.plusSeconds(5)));
        assertEquals(Distance.ofNauticalMiles(0), instance.horizontalSeparationAt(EPOCH.plusSeconds(10)));

        assertEquals(0.75, instance.horizontalSeparationAt(EPOCH.plusMillis(2_500)).inNauticalMiles(), TOLERANCE);
        assertEquals(0.1, instance.horizontalSeparationAt(EPOCH.plusSeconds(9)).inNauticalMiles(), TOLERANCE);
    }

    @Test
    public void test_horizontalDistanceAt_beforeTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.horizontalSeparationAt(EPOCH.minusSeconds(1)),
            "This should fail because this time is outside the timeWindow of these time series"
        );
    }

    @Test
    public void test_horizontalDistanceAt_afterTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.horizontalSeparationAt(EPOCH.plusSeconds(11)),
            "This should fail because this time is outside the timeWindow of these time series"
        );
    }

    @Test
    public void test_verticalClosureRate() {

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );
        Double TOL = 0.000001;

        //...separates at 100 ft every 5 seconds = 20ft per second
        assertEquals(-20.0, instance.verticalClosureRateAt(EPOCH).inFeetPerSecond(), TOL);
        assertEquals(-20.0, instance.verticalClosureRateAt(EPOCH.plusMillis(1125)).inFeetPerSecond(), TOL);
        assertEquals(-20.0, instance.verticalClosureRateAt(EPOCH.plusSeconds(5)).inFeetPerSecond(), TOL);
        assertEquals(-20.0, instance.verticalClosureRateAt(EPOCH.plusMillis(7234)).inFeetPerSecond(), TOL);
        assertEquals(-20.0, instance.verticalClosureRateAt(EPOCH.plusSeconds(10)).inFeetPerSecond(), TOL);
    }

    @Test
    public void test_verticalClosureRate_2() {

        Distance[] verticalDistances = new Distance[]{
            Distance.ofFeet(0),
            Distance.ofFeet(50),
            Distance.ofFeet(200)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances,
            horizontalDistances()
        );
        Double TOL = 0.000001;

        //the aircraft are separating, thus the closure rate is negative
        assertTrue(instance.verticalClosureRateAt(EPOCH).inFeetPerSecond() < 0);

        assertEquals(-10.0, instance.verticalClosureRateAt(EPOCH).inFeetPerSecond(), TOL);
        assertEquals(-10.0, instance.verticalClosureRateAt(EPOCH.plusMillis(1125)).inFeetPerSecond(), TOL);
        assertEquals(-30.0, instance.verticalClosureRateAt(EPOCH.plusSeconds(5)).inFeetPerSecond(), TOL);
        assertEquals(-30.0, instance.verticalClosureRateAt(EPOCH.plusMillis(7234)).inFeetPerSecond(), TOL);
        assertEquals(-30.0, instance.verticalClosureRateAt(EPOCH.plusSeconds(10)).inFeetPerSecond(), TOL);
    }

    @Test
    public void test_verticalClosureRate_beforeTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.verticalClosureRateAt(EPOCH.minusSeconds(1)),
            "This should fail because this time is outside the timeWindow of these time series"
        );
    }

    @Test
    public void test_verticalClosureRate_afterTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> instance.verticalClosureRateAt(EPOCH.plusSeconds(11)),
            "This should fail because this time is outside the timeWindow of these time series"
        );
    }

    @Test
    public void test_horizontalClosureRate() {

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );
        Double TOL = 0.000001;

        Speed closureRate = new Speed(Distance.ofNauticalMiles(0.5), Duration.ofSeconds(5));
        double rateInKnots = closureRate.inKnots();
        assertTrue(instance.horizontalClosureRateAt(EPOCH).inKnots() > 0);
        assertEquals(rateInKnots, instance.horizontalClosureRateAt(EPOCH).inKnots(), TOL);
        assertEquals(rateInKnots, instance.horizontalClosureRateAt(EPOCH.plusMillis(1125)).inKnots(), TOL);
        assertEquals(rateInKnots, instance.horizontalClosureRateAt(EPOCH.plusSeconds(5)).inKnots(), TOL);
        assertEquals(rateInKnots, instance.horizontalClosureRateAt(EPOCH.plusMillis(7234)).inKnots(), TOL);
        assertEquals(rateInKnots, instance.horizontalClosureRateAt(EPOCH.plusSeconds(10)).inKnots(), TOL);
    }

    @Test
    public void test_horizontalClosureRate_beforeTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.horizontalClosureRateAt(EPOCH.minusSeconds(1)),
            "This should fail because this time is outside the timeWindow of these time series"
        );

    }

    @Test
    public void test_horizontalClosureRate_afterTimeWindow() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.horizontalClosureRateAt(EPOCH.plusSeconds(11)),
            "This should fail because this time is outside the timeWindow of these time series"
        );
    }

    @Test
    public void test_timeUntilVerticalClosure_notClosing() {
        Distance[] verticalDistances = new Distance[]{
            Distance.ofFeet(100),
            Distance.ofFeet(100),
            Distance.ofFeet(200)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances,
            horizontalDistances()
        );

        //All these Optionals should be empty because this data is not closing vertically
        assertFalse(instance.timeUntilVerticalClosure(EPOCH.plusSeconds(0)).isPresent());
        assertFalse(instance.timeUntilVerticalClosure(EPOCH.plusSeconds(1)).isPresent());
        assertFalse(instance.timeUntilVerticalClosure(EPOCH.plusSeconds(5)).isPresent());
        assertFalse(instance.timeUntilVerticalClosure(EPOCH.plusSeconds(7)).isPresent());
        assertFalse(instance.timeUntilVerticalClosure(EPOCH.plusSeconds(10)).isPresent());
    }

    @Test
    public void test_timeUntilVerticalClosure_closing() {
        Distance[] verticalDistances = new Distance[]{
            Distance.ofFeet(200),
            Distance.ofFeet(100),
            Distance.ofFeet(50)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances,
            horizontalDistances()
        );

        assertEquals(Duration.ofSeconds(10), instance.timeUntilVerticalClosure(EPOCH.plusSeconds(0)).get());
        assertEquals(Duration.ofSeconds(9), instance.timeUntilVerticalClosure(EPOCH.plusSeconds(1)).get());
        assertEquals(Duration.ofSeconds(10), instance.timeUntilVerticalClosure(EPOCH.plusSeconds(5)).get());
        assertEquals(Duration.ofSeconds(8), instance.timeUntilVerticalClosure(EPOCH.plusSeconds(7)).get());
        assertEquals(Duration.ofSeconds(5), instance.timeUntilVerticalClosure(EPOCH.plusSeconds(10)).get());
    }

    @Test
    public void test_timeUntilHorizontalClosure_notClosing() {
        Distance[] horizontalDistances = new Distance[]{
            Distance.ofNauticalMiles(1.0),
            Distance.ofNauticalMiles(1.0),
            Distance.ofNauticalMiles(1.5)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances
        );

        //All these Optionals should be empty because this data is not closing horizontally
        assertFalse(instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(0)).isPresent());
        assertFalse(instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(1)).isPresent());
        assertFalse(instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(5)).isPresent());
        assertFalse(instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(7)).isPresent());
        assertFalse(instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(10)).isPresent());
    }

    @Test
    public void test_timeUntilHorizontalClosure_outsideRange() {
        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> instance.timeUntilHorizontalClosure(EPOCH.minusSeconds(1))
        );
    }

    @Test
    public void test_timeUntilHorizontalClosure_closing() {
        Distance[] horizontalDistances = new Distance[]{
            Distance.ofNauticalMiles(1),
            Distance.ofNauticalMiles(.5),
            Distance.ofNauticalMiles(.25)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances
        );

        assertEquals(Duration.ofSeconds(10), instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(0)).get());
        assertEquals(Duration.ofSeconds(9), instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(1)).get());
        assertEquals(Duration.ofSeconds(10), instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(5)).get());
        assertEquals(Duration.ofSeconds(8), instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(7)).get());
        assertEquals(Duration.ofSeconds(5), instance.timeUntilHorizontalClosure(EPOCH.plusSeconds(10)).get());
    }

    @Test
    public void testVerticalDistAtHorizontalClosureTime() {

        //increasing vertical separation...
        Distance[] verticalDistances = new Distance[]{
            Distance.ofFeet(200),
            Distance.ofFeet(300),
            Distance.ofFeet(500)
        };

        //decreasing horizontal separation...
        Distance[] horizontalDistances = new Distance[]{
            Distance.ofNauticalMiles(1),
            Distance.ofNauticalMiles(.5),
            Distance.ofNauticalMiles(.25)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances,
            horizontalDistances
        );

        assertEquals(
            Distance.ofFeet(400), //200ft + 200ft in new "closure"
            instance.verticalDistAtHorizontalClosureTime(EPOCH.plusSeconds(0)).get()
        );
        assertEquals(
            Distance.ofFeet(220 + 180), //220ft + 180 in new "closure"
            instance.verticalDistAtHorizontalClosureTime(EPOCH.plusSeconds(1)).get()
        );
        assertEquals(
            Distance.ofFeet(300 + 400), //300 ft + 10 seconds @ 200ft every 5 sec
            instance.verticalDistAtHorizontalClosureTime(EPOCH.plusSeconds(5)).get()
        );
        assertEquals(
            Distance.ofFeet(380 + 320), //380 ft + 8 seconds @ 200ft every 5 sec
            instance.verticalDistAtHorizontalClosureTime(EPOCH.plusSeconds(7)).get()
        );
        assertEquals(
            Distance.ofFeet(500 + 200), //500ft + 5 seconds @ 200ft every 5 sec
            instance.verticalDistAtHorizontalClosureTime(EPOCH.plusSeconds(10)).get()
        );
    }

    @Test
    public void testHorizontalDistAtVerticalClosureTime() {

        //decreasing vertical separation...
        Distance[] verticalDistances = new Distance[]{
            Distance.ofFeet(200),
            Distance.ofFeet(100),
            Distance.ofFeet(50)
        };

        //increasing horizontal separation...
        Distance[] horizontalDistances = new Distance[]{
            Distance.ofNauticalMiles(2),
            Distance.ofNauticalMiles(3),
            Distance.ofNauticalMiles(5)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances,
            horizontalDistances
        );

        assertEquals(
            Distance.ofNauticalMiles(2 + 2), //2 NM + 2 NM in new "closure"
            instance.horizontalDistAtVerticalClosureTime(EPOCH.plusSeconds(0)).get()
        );
        assertEquals(
            Distance.ofNauticalMiles(2.2 + 1.8), //2.2 NM + 1.8 NM in new "closure"
            instance.horizontalDistAtVerticalClosureTime(EPOCH.plusSeconds(1)).get()
        );
        assertEquals(
            Distance.ofNauticalMiles(3 + 4), //3 NM ft + 10 seconds @ 2 NM every 5 sec
            instance.horizontalDistAtVerticalClosureTime(EPOCH.plusSeconds(5)).get()
        );
        assertEquals(
            Distance.ofNauticalMiles(3.8 + 3.2), //3.8 NM + 8 seconds @ 2 NM every 5 sec
            instance.horizontalDistAtVerticalClosureTime(EPOCH.plusSeconds(7)).get()
        );
        assertEquals(
            Distance.ofNauticalMiles(5 + 2), //5 NM + 5 seconds @ 2 NM every 5 sec
            instance.horizontalDistAtVerticalClosureTime(EPOCH.plusSeconds(10)).get()
        );
    }

    @Test
    public void testPredictedVerticalSeparation() {
        Distance[] verticalDistances = new Distance[]{
            Distance.ofFeet(200),
            Distance.ofFeet(100),
            Distance.ofFeet(200)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances,
            horizontalDistances()
        );

        //start a 180 separation...should drop to 120 ft after 3 seconds
        assertEquals(
            120.0,
            instance.predictedVerticalSeparation(EPOCH.plusSeconds(1), Duration.ofSeconds(3)).inFeet(),
            0.000001
        );
        //start a 180 separation...should drop to 0 ft after 9 seconds
        assertEquals(
            0.0, //in feet
            instance.predictedVerticalSeparation(EPOCH.plusSeconds(1), Duration.ofSeconds(9)).inFeet(),
            0.000001
        );
        //start a 180 separation...should go back up to 20 ft after 10 seconds
        assertEquals(
            20.0, //in feet
            instance.predictedVerticalSeparation(EPOCH.plusSeconds(1), Duration.ofSeconds(10)).inFeet(),
            0.000001
        );
        //start a 120 separation...should go up to 320 ft after 10 seconds
        assertEquals(
            320.0, //in feet
            instance.predictedVerticalSeparation(EPOCH.plusSeconds(6), Duration.ofSeconds(10)).inFeet(),
            0.000001
        );
    }

    @Test
    public void testPredictedHorizontalSeparation() {
        Distance[] horizontalDistances = new Distance[]{
            Distance.ofNauticalMiles(1),
            Distance.ofNauticalMiles(.5),
            Distance.ofNauticalMiles(1)
        };

        SeparationTimeSeries instance = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances
        );

        //start a .9 separation...should drop to .6 NM after 3 seconds
        assertEquals(
            0.6,
            instance.predictedHorizontalSeparation(EPOCH.plusSeconds(1), Duration.ofSeconds(3)).inNauticalMiles(),
            0.000001
        );
        //start a .9 separation...should drop to 0 NM after 9 seconds
        assertEquals(
            0.0, //nautical miles
            instance.predictedHorizontalSeparation(EPOCH.plusSeconds(1), Duration.ofSeconds(9)).inNauticalMiles(),
            0.000001
        );
        //start a .9 separation...should go back up to .1 NM after 10 seconds
        assertEquals(
            0.1, //nautical miles
            instance.predictedHorizontalSeparation(EPOCH.plusSeconds(1), Duration.ofSeconds(10)).inNauticalMiles(),
            0.000001
        );
        //start a .6 separation...should go up to 1.6 NM after 10 seconds
        assertEquals(
            1.6, //nautical miles
            instance.predictedHorizontalSeparation(EPOCH.plusSeconds(6), Duration.ofSeconds(10)).inNauticalMiles(),
            0.000001
        );
    }

    @Test
    public void testSerialization() {

        SeparationTimeSeries timeSeries = new SeparationTimeSeries(
            times(),
            verticalDistances(),
            horizontalDistances()
        );

        /*
         * @TODO This is a shit test Should be able to use
         * org.mitre.caasd.commons.testing.TestUtils.serializeAndDeserialize(instance) But this
         * utility isn't visible -- probably because it was declared in the test code..not sure
         *
         * This crap test will have to do for now
         */
        assertTrue(timeSeries instanceof Serializable);
    }
}
