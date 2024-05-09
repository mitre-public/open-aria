
package org.mitre.openaria.pointpairing;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.collect.DistanceMetric;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;

import org.junit.jupiter.api.Test;

public class PointPairFinderTest {

    /**
     * A TestSink gives you a way to confirm that the PointPairFinder appropriately "published"
     * Point Pairs.
     */
    static class TestSink implements Consumer<Pair<Point, Point>> {

        Pair<Point, Point> lastPair = null;

        int count = 0;

        @Override
        public void accept(Pair<Point, Point> t) {
            this.lastPair = t;
            count++;
        }
    }

    /**
     * Test of accept method, of class PointPairFinder.
     */
    @Test
    public void testAccept() {

        DistanceMetric<Point> metric = new PointDistanceMetric(1.0, 1.0);
        double DISTANCE_THRESHOLD = 1250.0;
        TestSink sink = new TestSink();

        PointPairFinder pairer = new PointPairFinder(Duration.ofSeconds(13), metric, DISTANCE_THRESHOLD, sink);

        Instant time1 = Instant.EPOCH;
        Instant time2 = time1.plusSeconds(1);
        Instant time3 = time2.plusSeconds(1);

        Point p1 = (new PointBuilder())
            .time(time1).latLong(0.0, 0.0).altitude(Distance.ofFeet(0.0)).build();

        Point p1_plusAltitude = (new PointBuilder())
            .time(time1).latLong(0.0, 0.0).altitude(Distance.ofFeet(10.0)).build();

        Point p2 = (new PointBuilder())
            .time(time2).latLong(0.0, 0.0).altitude(Distance.ofFeet(0.0)).build();

        Point p3 = (new PointBuilder())
            .time(time3).latLong(0.0, 0.0).altitude(Distance.ofFeet(0.0)).build();

        pairer.accept(p1);

        assertEquals(
            0, sink.count,
            "There should be no pairs yet"
        );

        pairer.accept(p1_plusAltitude);

        assertEquals(
            1, sink.count,
            "p1_plusAltitude is only a few feet above the first point. It should be within range."
        );

        //verify these points pair SHOULD be found
        assertTrue(metric.distanceBtw(p1, p2) < DISTANCE_THRESHOLD);
        assertTrue(metric.distanceBtw(p1_plusAltitude, p2) < DISTANCE_THRESHOLD);

        //verify these points pair ARE found
        int priorPairCount = sink.count;
        pairer.accept(p2);

        assertEquals(
            2, sink.count - priorPairCount,
            "Two new pairs should have been found"
        );

        //verify this point pair SHOULD be found
        assertTrue(metric.distanceBtw(p2, p3) < DISTANCE_THRESHOLD);

        //verify these points pair ARE found
        priorPairCount = sink.count;
        pairer.accept(p3);

        assertEquals(
            1, sink.count - priorPairCount,
            "1 pair should have been found"
        );
    }

    @Test
    public void testUnorderedPoints() {

        DistanceMetric<Point> metric = new PointDistanceMetric(1.0, 1.0);
        double DISTANCE_THRESHOLD = 1250.0;
        TestSink sink = new TestSink();

        PointPairFinder pairer = new PointPairFinder(Duration.ofSeconds(13), metric, DISTANCE_THRESHOLD, sink);

        Instant time1 = Instant.EPOCH;
        Instant time2 = Instant.EPOCH.minusSeconds(1);

        Point p1 = (new PointBuilder())
            .time(time1).latLong(0.0, 0.0).altitude(Distance.ofFeet(0.0)).build();

        Point p2 = (new PointBuilder())
            .time(time2).latLong(0.0, 0.0).altitude(Distance.ofFeet(0.0)).build();

        pairer.accept(p1);

        assertThrows(
            IllegalArgumentException.class,
            () -> pairer.accept(p2),
            "Fail because input data is not sorted by time"
        );
    }
}
