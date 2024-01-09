
package org.mitre.openaria.pointpairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.Spherical;

public class DistanceFilterTest {

    private static final double MAX_DISTANCE_IN_FEET = 1000;
    private static final long MAX_TIME_DELTA_IN_MILLISEC = 1000;

    private static DistanceFilter newTestFilter() {
        return new DistanceFilter(MAX_DISTANCE_IN_FEET, MAX_TIME_DELTA_IN_MILLISEC);
    }

    /**
     * Case 1: Fail Time, Fail Distance
     */
    @Test
    public void testCase1() {

        DistanceFilter filter = newTestFilter();

        LatLong position1 = new LatLong(0.0, 0.0);
        double tooFarInNm = MAX_DISTANCE_IN_FEET * 3.0 / Spherical.feetPerNM();

        Point p1 = new PointBuilder()
            .latLong(position1)
            .time(Instant.EPOCH)
            .altitude(Distance.ofFeet(500.0))
            .build();

        Point p2 = new PointBuilder()
            .latLong(position1.projectOut(90.0, tooFarInNm)) //move the position
            .time(Instant.EPOCH.plusMillis(MAX_TIME_DELTA_IN_MILLISEC * 2))
            .altitude(Distance.ofFeet(500.0))
            .build();

        assertFalse(filter.test(Pair.of(p1, p2)));
        assertFalse(filter.test(Pair.of(p2, p1)));
    }

    /**
     * Case 1: Fail Time, Pass Distance
     */
    @Test
    public void testCase2() {

        DistanceFilter filter = newTestFilter();

        LatLong position1 = new LatLong(0.0, 0.0);
        double notTooFarInNm = MAX_DISTANCE_IN_FEET * 0.5 / Spherical.feetPerNM();

        Point p1 = new PointBuilder()
            .latLong(position1)
            .time(Instant.EPOCH)
            .altitude(Distance.ofFeet(500.0))
            .build();

        Point p2 = new PointBuilder()
            .latLong(position1.projectOut(90.0, notTooFarInNm)) //move the position
            .time(Instant.EPOCH.plusMillis(MAX_TIME_DELTA_IN_MILLISEC * 2))
            .altitude(Distance.ofFeet(500.0))
            .build();

        assertFalse(filter.test(Pair.of(p1, p2)));
        assertFalse(filter.test(Pair.of(p2, p1)));
    }

    /**
     * Case 3: Pass Time, Fail Distance
     */
    @Test
    public void testCase3() {

        DistanceFilter filter = newTestFilter();

        LatLong position1 = new LatLong(0.0, 0.0);
        double tooFarInNm = MAX_DISTANCE_IN_FEET * 3.0 / Spherical.feetPerNM();

        Point p1 = new PointBuilder()
            .latLong(position1)
            .time(Instant.EPOCH)
            .altitude(Distance.ofFeet(500.0))
            .build();

        Point p2 = new PointBuilder()
            .latLong(position1.projectOut(90.0, tooFarInNm)) //move the position
            .time(Instant.EPOCH.plusMillis(MAX_TIME_DELTA_IN_MILLISEC / 2))
            .altitude(Distance.ofFeet(500.0))
            .build();

        assertFalse(filter.test(Pair.of(p1, p2)));
        assertFalse(filter.test(Pair.of(p2, p1)));
    }

    /**
     * Case 4: Pass Time, Pass Distance
     */
    @Test
    public void testCase4() {

        DistanceFilter filter = newTestFilter();

        LatLong position1 = new LatLong(0.0, 0.0);
        double notTooFarInNm = MAX_DISTANCE_IN_FEET * 0.5 / Spherical.feetPerNM();

        Point p1 = new PointBuilder()
            .latLong(position1)
            .time(Instant.EPOCH)
            .altitude(Distance.ofFeet(500.0))
            .build();

        Point p2 = new PointBuilder()
            .latLong(position1.projectOut(90.0, notTooFarInNm)) //move the position
            .time(Instant.EPOCH.plusMillis(MAX_TIME_DELTA_IN_MILLISEC / 2))
            .altitude(Distance.ofFeet(500.0))
            .build();

        assertTrue(filter.test(Pair.of(p1, p2)));
        assertTrue(filter.test(Pair.of(p2, p1)));
    }

}
