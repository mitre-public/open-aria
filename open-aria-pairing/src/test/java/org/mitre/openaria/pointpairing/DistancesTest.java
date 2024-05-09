
package org.mitre.openaria.pointpairing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;

import org.junit.jupiter.api.Test;

public class DistancesTest {

    @Test
    public void testEstimateDistanceInFeet() {

        //1 knot -- due east
        Point testPoint = (new PointBuilder())
            .time(Instant.EPOCH)
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .speedInKnots(1.0)
            .courseInDegrees(90.0)
            .build();

        //1 knot -- due west (1 second later)
        Point testPoint2 = (new PointBuilder())
            .time(Instant.EPOCH.plusSeconds(1L))
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(77.0)) //I am higher
            .speedInKnots(1.0)
            .courseInDegrees(270.0)
            .build();

        long MAX_TIME_DELTA = 1000L;

        double estimatedDist = Distances.estimateDistanceInFeet(testPoint, testPoint2, MAX_TIME_DELTA);

        assertEquals(
            77.0, estimatedDist, 0.0000001,
            "The Points should be at exactly that same Lat/Long at time = .5 sec"
        );

    }

    @Test
    public void testEstimateDistanceInFeet_tooFarApartInTime() {

        //1 knot -- due east
        Point testPoint = (new PointBuilder())
            .time(Instant.EPOCH)
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .speedInKnots(1.0)
            .courseInDegrees(90.0)
            .build();

        //1 knot -- due west (1 minute later)
        Point testPoint2 = (new PointBuilder())
            .time(Instant.EPOCH.plusSeconds(60L))
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .speedInKnots(1.0)
            .courseInDegrees(270.0)
            .build();

        long MAX_TIME_DELTA = 1000L;

        assertThrows(
            IllegalArgumentException.class,
            () -> Distances.estimateDistanceInFeet(testPoint, testPoint2, MAX_TIME_DELTA),
            "Should have failed because 1 minute is too much"
        );
    }

    @Test
    public void testProjectPointAtNewTime() {

        Point testPoint = (new PointBuilder())
            .time(Instant.EPOCH)
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .build();

        Instant newTime = Instant.EPOCH.plusSeconds(1L);

        Point projection = Distances.projectPointAtNewTime(testPoint, newTime);

        assertTrue(projection.time().equals(newTime));
        assertTrue(projection != testPoint);
        assertTrue(projection.latLong().latitude() == 0.0);
        assertTrue(projection.latLong().longitude() == 0.0);
        assertTrue(projection.altitude().inFeet() == 0.0);
    }

    @Test
    public void testProjectPointAtNewTime_forwardInTime() {

        //1 knot -- due east
        Point testPoint = (new PointBuilder())
            .time(Instant.EPOCH)
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .speedInKnots(1.0)
            .courseInDegrees(90.0)
            .build();

        Instant newTime = Instant.EPOCH.plusSeconds(60 * 60);

        Point projection = Distances.projectPointAtNewTime(testPoint, newTime);

        assertTrue(projection.time().equals(newTime));
        assertTrue(projection != testPoint);
        assertEquals(0.0, projection.latLong().latitude(), 0.00001, "Latitude should be 0");
        assertTrue(projection.latLong().longitude() > 0.0, "Longitude should be positive");
        assertTrue(projection.altitude().inFeet() == 0.0);

        LatLong start = new LatLong(0.0, 0.0);
        LatLong end = projection.latLong();

        assertEquals(
            1.0, start.distanceInNM(end), 0.0001,
            "Traveling 1 hour at 1 knot should move 1 NM"
        );
    }

    @Test
    public void testProjectPointAtNewTime_backwardInTime() {

        //1 knot -- due east
        Point testPoint = (new PointBuilder())
            .time(Instant.EPOCH)
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .speedInKnots(1.0)
            .courseInDegrees(90.0)
            .build();

        Instant newTime = Instant.EPOCH.minusSeconds(60 * 60);

        Point projection = Distances.projectPointAtNewTime(testPoint, newTime);

        assertTrue(projection.time().equals(newTime));
        assertTrue(projection != testPoint);
        assertEquals(0.0, projection.latLong().latitude(), 0.00001, "Latitude should be 0");
        assertTrue(projection.latLong().longitude() < 0.0, "Longitude should be positive");
        assertTrue(projection.altitude().inFeet() == 0.0);

        LatLong start = new LatLong(0.0, 0.0);
        LatLong end = projection.latLong();

        assertEquals(
            1.0, start.distanceInNM(end), 0.0001,
            "Traveling 1 hour at 1 knot should move 1 NM"
        );
    }

}
