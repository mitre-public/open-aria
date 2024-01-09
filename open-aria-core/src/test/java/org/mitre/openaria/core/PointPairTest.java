
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;


public class PointPairTest {

    @Test
    public void testConstructors() {

        Point p1 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,024,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");
        Point p2 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,034,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");

        PointPair pair = new PointPair(p1, p2);

        assertEquals(pair.point1(), p1);
        assertEquals(pair.point2(), p2);
    }

    @Test
    public void testStaticFactory() {

        Point p1 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,024,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");
        Point p2 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,034,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");

        PointPair pair = PointPair.of(p1, p2);

        assertEquals(pair.point1(), p1);
        assertEquals(pair.point2(), p2);
    }

    @Test
    public void verifyLateralDistanceIsIgnoredIfAltitudeIsTooDifferent() {
        Distance requiredAltitudeProximity = Distance.ofFeet(500);

        //alt = 2400
        Point p1 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,024,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");
        //alt = 3400
        Point p2 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,034,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");

        assertEquals(p1.altitude().inFeet(), 2400.0, 0.01);
        assertEquals(p2.altitude().inFeet(), 3400.0, 0.01);

        PointPair pair = new PointPair(p1, p2);

        //IMPORTANT: no NPE is thrown...so we know we didn't compute the lateral distance
        assertFalse(pair.areWithin(requiredAltitudeProximity, null));
    }

    @Test
    public void testAltitudeDelta() {
        //alt = 2400
        Point p1 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,024,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");
        //alt = 3400
        Point p2 = NopPoint.from("[RH],STARS,A80_B,02/12/2018,18:36:46.667,JIA5545,CRJ9,E,5116,034,157,270,033.63143,-084.33913,1334,5116,22.4031,27.6688,1,O,A,A80,OZZ,OZZ,ATL,1827,ATL,ACT,IFR,,01719,,,,,27L,L,1,,0,{RH}");

        PointPair pair = new PointPair(p1, p2);

        assertTrue(pair.altitudeDelta().equals(Distance.ofFeet(1_000)));
    }

    @Test
    public void testLateralDistance() {
        Point p1 = Point.builder()
            .latLong(LatLong.of(0.0, 0.0))
            .time(EPOCH)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(0.0, 10.0)) //move 10 NM North
            .time(EPOCH)
            .build();

        PointPair pair = new PointPair(p1, p2);

        Distance expectedDist = Distance.ofNauticalMiles(10.0);
        Distance actualDist = pair.lateralDistance();
        Distance error = actualDist.minus(expectedDist).abs();
        Distance tolerance = Distance.ofNauticalMiles(0.001);

        assertTrue(error.isLessThan(tolerance));
    }

    @Test
    public void testAvgLatLong() {

        Point p1 = Point.builder().latLong(33.63143, -84.33913).build();
        Point p2 = Point.builder().latLong(33.64143, -84.43913).build();

        PointPair pair = PointPair.of(p1, p2);

        LatLong actual = pair.avgLatLong();
        LatLong expected = LatLong.of(33.63643, -84.38913);

        assertTrue(expected.distanceTo(actual).isLessThan(Distance.ofNauticalMiles(0.00001)));
    }

    @Test
    public void testAvgAltitude() {

        Point p1 = Point.builder().altitude(Distance.ofFeet(1000.0)).build();
        Point p2 = Point.builder().altitude(Distance.ofFeet(1500.0)).build();

        PointPair pair = PointPair.of(p1, p2);

        assertEquals(
            Distance.ofFeet(1250.0),
            pair.avgAltitude()
        );
    }

    @Test
    public void testMagnitudeOfVelocityDelta_bothMoving() throws Exception {
        // points are driving at each other
        Point p1 = Point.builder().speed(20.0).courseInDegrees(60.0).build();
        Point p2 = Point.builder().speed(35.0).courseInDegrees(240.0).build();

        PointPair pair = PointPair.of(p1, p2);

        assertEquals(
            pair.magnitudeOfVelocityDelta().inKnots(), 55.0, 1E-6,
            "Incorrect velocity magnitude delta."
        );
    }

    @Test
    public void testMagnitudeOfVelocityDelta_oneStationary() throws Exception {
        Point p1 = Point.builder().speed(0.0).courseInDegrees(100.0).build();
        Point p2 = Point.builder().speed(50.0).courseInDegrees(240.0).build();

        PointPair pair = PointPair.of(p1, p2);
        assertEquals(
            pair.magnitudeOfVelocityDelta().inKnots(), 50.0, 1E-6,
            "Incorrect velocity magnitude delta."
        );
    }

    @Test
    public void testHorizontalClosestPointOfApproach() {
        Point p1 = Point.builder()
            .latLong(LatLong.of(0.0, 0.0))
            .time(EPOCH)
            .courseInDegrees(90.0)
            .speed(1.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(90.0, 10.0)) //move 10 NM East
            .time(EPOCH)
            .courseInDegrees(270.0)
            .speed(1.0)
            .build();

        PointPair pair = new PointPair(p1, p2);

        ClosestPointOfApproach closestPoint = pair.closestPointOfApproach();

        assertEquals(5.0 * 1000 * 3600, closestPoint.timeUntilCpa().toMillis(), 1.0);
        assertEquals(0.0, closestPoint.distanceAtCpa().inNauticalMiles(), 0.0001);
    }

    @Test
    public void testVerticalClosestPointOfApproach() {
        Point p1 = Point.builder()
            .latLong(LatLong.of(0.0, 0.0))
            .time(EPOCH)
            .courseInDegrees(0.0)
            .speed(1.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(0.0, 10.0)) //move 10 NM East
            .time(EPOCH)
            .courseInDegrees(180.0)
            .speed(1.0)
            .build();

        PointPair pair = new PointPair(p1, p2);

        ClosestPointOfApproach closestPoint = pair.closestPointOfApproach();

        assertEquals(5.0 * 1000 * 3600, closestPoint.timeUntilCpa().toMillis(), 1.0);
        assertEquals(0.0, closestPoint.distanceAtCpa().inNauticalMiles(), 0.0001);
    }

    @Test
    public void testOvertakingClosestPointOfApproach() {
        /*
         * The first point is moving North at 2 knots and will overtake the second point which is
         * moving North at 1 knot.
         */
        Point p1 = Point.builder()
            .latLong(LatLong.of(0.0, 0.0))
            .time(EPOCH)
            .courseInDegrees(0.0)
            .speed(2.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(0.0, 10.0)) //move 10 NM East
            .time(EPOCH)
            .courseInDegrees(0.0)
            .speed(1.0)
            .build();

        PointPair pair = new PointPair(p1, p2);

        ClosestPointOfApproach closestPoint = pair.closestPointOfApproach();

        assertEquals(10.0 * 1000 * 3600, closestPoint.timeUntilCpa().toMillis(), 1.0);
        assertEquals(0.0, closestPoint.distanceAtCpa().inNauticalMiles(), 0.0001);
    }

    @Test
    public void testDueToCollideClosestPointOfApproach() {
        Point p1 = Point.builder()
            .latLong(LatLong.of(0.0, 0.0))
            .time(EPOCH)
            .courseInDegrees(45.0)
            .speed(1.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(45.0, 10.0)) //move 10 NM East
            .time(EPOCH)
            .courseInDegrees(225.0)
            .speed(1.0)
            .build();

        PointPair pair = new PointPair(p1, p2);

        ClosestPointOfApproach closestPoint = pair.closestPointOfApproach();

        assertEquals(5.0 * 1000 * 3600, closestPoint.timeUntilCpa().toMillis(), 10.0);
        assertEquals(0.0, closestPoint.distanceAtCpa().inNauticalMiles(), 0.0002);
    }

    @Test
    public void testFlyingPastEachOtherClosestPointOfApproach() {
        Point p1 = Point.builder()
            .latLong(LatLong.of(0.0, 0.0))
            .time(EPOCH)
            .courseInDegrees(45.0)
            .speed(1.0)
            .build();

        LatLong p2latLong = p1.latLong()
            .projectOut(45.0, 10.0) // move 10 NM NE
            .projectOut(135.0, 1.0); // move 1 NM SE

        Point p2 = Point.builder()
            .latLong(p2latLong)
            .time(EPOCH)
            .courseInDegrees(225.0)
            .speed(1.0)
            .build();

        PointPair pair = new PointPair(p1, p2);

        ClosestPointOfApproach cpa = pair.closestPointOfApproach();

        assertEquals(5.0 * 1000 * 3600, cpa.timeUntilCpa().toMillis(), 4.0);
        assertEquals(1.0, cpa.distanceAtCpa().inNauticalMiles(), 0.0001);
    }

    /**
     * This test (and the next few) have p1 being at the start of runway 33 at DCA, with a heading
     * of 330 deg (meaning along the runway). The opposite direction is 150 deg.
     */
    @Test
    public void testHorizontalClosure_WhenExactlyConverging_ReturnSumOfSpeeds() {

        Point p1 = Point.builder()
            .latLong(LatLong.of(38.851110, -77.033495))
            .courseInDegrees(330.0)
            .speed(30.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(330.0, 0.1))
            .courseInDegrees(150.0)
            .speed(45.0)
            .build();

        assertEquals(30.0 + 45.0, PointPair.of(p1, p2).horizontalClosure().inKnots(), 1E-3);
    }

    @Test
    public void testHorizontalClosure_WhenExactlyDiverging_ReturnNegativeSumOfSpeeds() {

        Point p1 = Point.builder()
            .latLong(LatLong.of(38.851110, -77.033495))
            .courseInDegrees(150.0)
            .speed(30.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(330.0, 0.1))
            .courseInDegrees(330.0)
            .speed(45.0)
            .build();

        assertEquals(-(30.0 + 45.0), PointPair.of(p1, p2).horizontalClosure().inKnots(), 1E-3);
    }

    @Test
    public void testHorizontalClosure_WhenParallelAndSameSpeed_ReturnZero() {

        Point p1 = Point.builder()
            .latLong(LatLong.of(38.851110, -77.033495))
            .courseInDegrees(330.0)
            .speed(30.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(330.0, 0.1))
            .courseInDegrees(330.0)
            .speed(30.0)
            .build();

        assertEquals(0.0, PointPair.of(p1, p2).horizontalClosure().inKnots(), 1E-3);
    }

    @Test
    public void testHorizontalClosure_WhenTooCloseToBeDetermined_ReturnZero() {

        Point p1 = Point.builder()
            .latLong(LatLong.of(38.851110, -77.033495))
            .courseInDegrees(330.0)
            .speed(30.0)
            .build();

        Point p2 = Point.builder()
            .latLong(LatLong.of(38.851104, -77.033498))
            .courseInDegrees(300.0)
            .speed(33.0)
            .build();

        assertEquals(0.0, PointPair.of(p1, p2).horizontalClosure().inKnots(), 1E-3);
    }

    @Test
    public void testHorizontalClosure_WhenOneIsStopped() {

        Point p1 = Point.builder()
            .latLong(LatLong.of(38.851110, -77.033495))
            .courseInDegrees(330.0)
            .speed(37.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(330.0, 0.2))
            .courseInDegrees(100.0)
            .speed(0.0)
            .build();

        assertEquals(37.0, PointPair.of(p1, p2).horizontalClosure().inKnots(), 1E-3);
    }

    @Test
    public void testHorizontalClosure_WhenBothAreStopped() {

        Point p1 = Point.builder()
            .latLong(LatLong.of(38.851110, -77.033495))
            .courseInDegrees(330.0)
            .speed(0.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(330.0, 0.2))
            .courseInDegrees(150.0)
            .speed(0.0)
            .build();

        assertEquals(0.0, PointPair.of(p1, p2).horizontalClosure().inKnots(), 1E-3);
    }

    @Test
    public void closestPointOfApproachRequiresPointsFromTheSameTime() {
        Point p1 = Point.builder().time(EPOCH).build();
        Point p2 = Point.builder().time(EPOCH.plusSeconds(1)).build();

        PointPair points = new PointPair(p1, p2);

        assertThrows(
            IllegalStateException.class,
            () -> points.closestPointOfApproach(),
            "The points have to be from the same time."
        );
    }

    @Test
    public void testDivergingPathsClosestPointOfApproach() {
        Point p1 = Point.builder()
            .latLong(LatLong.of(0.0, 0.0))
            .time(EPOCH)
            .courseInDegrees(45.0)
            .speed(1.0)
            .build();

        Point p2 = Point.builder()
            .latLong(p1.latLong().projectOut(270.0, 2.0))
            .time(EPOCH)
            .courseInDegrees(270.0)
            .speed(1.0)
            .build();

        PointPair points = new PointPair(p1, p2);

        ClosestPointOfApproach cpa = points.closestPointOfApproach();

        assertEquals(0.0, cpa.timeUntilCpa().toMillis(), 2.0);
        assertEquals(2.0, cpa.distanceAtCpa().inNauticalMiles(), 0.0001);
    }
}
