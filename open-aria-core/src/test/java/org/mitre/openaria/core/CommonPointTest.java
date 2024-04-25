
package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

import org.junit.jupiter.api.Test;

public class CommonPointTest {

    private static final double TOLERANCE = 0.0001;

    @Test
    public void testCompareTo() {
        /*
         * Verify that points are sorted by time.
         */

        Instant start = Instant.EPOCH;
        Instant middle = start.plusSeconds(7);
        Instant end = Instant.EPOCH.plusSeconds(15);

        CommonPoint startPoint = new PointBuilder().time(start).build();
        CommonPoint middlePoint = new PointBuilder().time(middle).build();
        CommonPoint endPoint = new PointBuilder().time(end).build();

        LinkedList<CommonPoint> points = new LinkedList<>();
        points.add(middlePoint);
        points.add(endPoint);
        points.add(startPoint);

        Collections.sort(points);

        assertTrue(points.getFirst() == startPoint);
        assertTrue(points.getLast() == endPoint);
    }

    @Test
    public void pointBuilderCanAddLatLong() {
        LatLong LAT_LONG = LatLong.of(10.0, 15.0);

        CommonPoint instance = new PointBuilder()
            .latLong(LAT_LONG)
            .build();

        assertEquals(LAT_LONG, instance.latLong());
    }

    @Test
    public void getAltitudeInFeet() {
        /*
         * Verify that altitudeInFeet is assigned correctly.
         */

        double ALTITUDE_IN_FEET = 15.0;

        CommonPoint instance = new PointBuilder()
            .altitude(Distance.ofFeet(ALTITUDE_IN_FEET))
            .build();

        assertEquals(ALTITUDE_IN_FEET, instance.altitude().inFeet(), TOLERANCE);
    }

    @Test
    public void testGetTime() {
        /*
         * Verify that time is assigned correctly.
         */
        Instant TIME = Instant.EPOCH.plusSeconds(22);
        // using a custom value because we don't want any flywieght crap screwing up this test

        CommonPoint instance = new PointBuilder()
            .time(TIME)
            .build();

        assertTrue(instance.time() == TIME);
    }

    @Test
    public void testEmptyString() {
        /*
         * Verify that all "String fields" cannot use the empty String "" as its value. This is
         * prohibited because it is too easily confused with null.
         *
         * Was the field intentionally set to ""? Or did a parsing step have no data and returned
         * ""?
         */

        for (PointField field : PointField.values()) {
            if (field.expectedType == String.class) {

                HashMap<PointField, Object> map = new HashMap<>();
                map.put(field, "");

                try {
                    new CommonPoint(map, null, null, null, null, null);
                    fail();
                } catch (IllegalStateException ise) {
                    //skipped the fail above
                }
            }
        }
    }

    // @todo -- Create tests that (A) confirm ordering rules on deduction based methods
    // @todo -- Create a test that confirms course deduction
    // @todo -- Create a test that confirms speed deduction
    // @todo -- Create a test for the speed and course interpolations..
}
