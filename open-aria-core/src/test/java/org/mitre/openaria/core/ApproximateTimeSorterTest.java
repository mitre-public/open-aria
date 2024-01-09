
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;

import com.google.common.collect.Lists;

public class ApproximateTimeSorterTest {

    /*
     * This list of partially sorted data can (A) be sorted properly on demand or (B) not get sorted
     * by a TimeBasedSorter if the "alloted lag" is too small.
     */
    private static List<Point> testData() {

        ArrayList<Instant> times = Lists.newArrayList(
            EPOCH,
            EPOCH.plusSeconds(1),
            EPOCH.plusSeconds(10),
            EPOCH.plusSeconds(5),
            EPOCH.plusSeconds(4),
            EPOCH.plusSeconds(11),
            EPOCH.plusSeconds(12),
            EPOCH.plusSeconds(13),
            EPOCH.plusSeconds(23)
        );

        ArrayList<Point> points = new ArrayList<>();
        for (Instant aTime : times) {
            points.add(new PointBuilder().time(aTime).build());
        }
        return points;
    }

    @Test
    public void testAllPointsWithinWindow() {
        /*
         * Confirm that no points are emitted when all the points occur within the time window
         */
        Duration maxLag = Duration.ofMinutes(5);

        ApproximateTimeSorter<Point> sorter = new ApproximateTimeSorter<>(
            maxLag,
            (Point t) -> {
                throw new UnsupportedOperationException("No Point should be forward to this Consumer");
            }
        );

        for (Point commonPoint : testData()) {
            sorter.accept(commonPoint);
        }
    }

    /**
     * This consumer verifies that CommonPoints passed to it are received in time-sorted order
     */
    class TimeOrderVerifyingConsumer implements Consumer<Point> {

        LinkedList<Point> points = new LinkedList<>();

        @Override
        public void accept(Point t) {

            if (!points.isEmpty()) {
                Point lastPoint = points.getLast();
                assertTrue(
                    lastPoint.time().isBefore(t.time())
                );
            }

            points.addLast(t);
        }

        public int size() {
            return points.size();
        }

        public Instant timeFor(int i) {
            return points.get(i).time();
        }
    }

    @Test
    public void testEmittedPointsAreProperlySorted() {

        Duration maxLag = Duration.ofSeconds(10L); //with this lag things get sorted correctly

        TimeOrderVerifyingConsumer consumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter sorter = new ApproximateTimeSorter(
            maxLag,
            consumer
        );

        for (Point commonPoint : testData()) {
            sorter.accept(commonPoint);
        }

        assertEquals(7, consumer.size());
    }

    @Test
    public void testEmittedPointsAreNotProperlySorted() {

        //with this lag things do not get sorted correctly
        Duration maxLag = Duration.ofSeconds(2L);

        TimeOrderVerifyingConsumer consumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter<Point> sorter = new ApproximateTimeSorter<>(maxLag, consumer);

        sorter.accept(new PointBuilder().time(EPOCH).build());
        assertEquals(0, consumer.size());

        sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(1)).build());
        assertEquals(0, consumer.size());

        sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(10)).build());
        assertEquals(2, consumer.size());
        assertEquals(consumer.timeFor(0), EPOCH);
        assertEquals(consumer.timeFor(1), EPOCH.plusSeconds(1));

        sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(5)).build());
        assertEquals(3, consumer.size());
        assertEquals(consumer.timeFor(2), EPOCH.plusSeconds(5));

        //this TestConsumer fails here because this point is passed through the sorter (after the 5 point was)
        assertThrows(
            AssertionError.class,
            () -> sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(4)).build())
        );
    }

    @Test
    public void testFlush() {
        /**
         * Confirm all points are emitted in the proper order upon calling "flush"
         */
        Duration maxLag = Duration.ofMinutes(5);

        TimeOrderVerifyingConsumer consumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter sorter = new ApproximateTimeSorter(
            maxLag,
            consumer
        );

        for (Point commonPoint : testData()) {
            sorter.accept(commonPoint);
        }

        assertEquals(0, consumer.size());

        sorter.flush();

        assertEquals(9, consumer.size());
    }

    @Test
    public void testOverflowBug() {
        /*
         * It should not be possible to crash an ApproximateTimeSorter by overloading the memory by
         * continuously providing older and older points.
         */

        Duration maxLag = Duration.ofSeconds(3);  //an extremely small sorting window

        ConsumingArrayList<Point> downstreamConsumer = newConsumingArrayList();

        ApproximateTimeSorter sorter = new ApproximateTimeSorter(maxLag, downstreamConsumer);

        //add Points to the sorter that keep getting older and older
        int NUM_POINTS = 50_000;
        for (int i = 0; i < NUM_POINTS; i++) {
            Instant time = EPOCH.minusSeconds(i);
            sorter.accept(new PointBuilder().time(time).build());
        }
        int numPointsPastSorter = downstreamConsumer.size();

        /*
         * We want most of the points to pass this sorter because this stream of "perfectly out of
         * order" data should not: (1) lead to an OutOfMemoryException or (2) be a fixable input
         * type. In other words, this data is so bad it isn't reasonable to get it organized.
         */
        assertTrue(numPointsPastSorter >= NUM_POINTS * .99);
    }

    @Test
    public void testVeryOldPointsAreAutoEvicted() {
        ConsumingArrayList<Point> downstreamConsumer = newConsumingArrayList();

        ApproximateTimeSorter sorter = new ApproximateTimeSorter(
            Duration.ofSeconds(10),
            downstreamConsumer
        );

        sorter.accept(Point.builder().time(EPOCH).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(4)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(8)).build());
        assertThat("no evictions yet", downstreamConsumer, empty());

        //Adding this point evicts the 1st point because it is more than 10 seconds older than the most recent input
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(12)).build()); //evict Epoch + 0
        assertThat(downstreamConsumer, hasSize(1));
        assertThat(downstreamConsumer.get(0).time(), equalTo(EPOCH));

        //This stale point should get instantly evicted because it is too old (with respect to time highwater mark)
        sorter.accept(Point.builder().time(EPOCH.minusSeconds(20)).build());
        assertThat(downstreamConsumer, hasSize(2));
        assertThat(downstreamConsumer.get(1).time(), equalTo(EPOCH.minusSeconds(20)));
    }

    @Test
    public void testStalePointsAreNotAutoEvicted() {

        TimeOrderVerifyingConsumer downstreamConsumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter sorter = new ApproximateTimeSorter(
            Duration.ofSeconds(10),
            downstreamConsumer
        );

        sorter.accept(Point.builder().time(EPOCH).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(4)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(8)).build());
        assertEquals(0, downstreamConsumer.points.size());

        //Adding this point should evict the 1st point because it is more than 10 seconds older than the most recent input
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(12)).build()); //evict Epoch + 0
        assertEquals(1, downstreamConsumer.points.size());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(16)).build()); //evict Epoch + 4
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(20)).build()); //evict Epoch + 8
        assertEquals(3, downstreamConsumer.points.size());

        //none of these "semi-stale" points are auto evicted because are in the "wait for more data" window
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(14)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(13)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(12)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(11)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(10)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(9)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(8)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(7)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(6)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(5)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(4)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(3)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(2)).build());
        sorter.accept(Point.builder().time(EPOCH.plusSeconds(1)).build());
        sorter.accept(Point.builder().time(EPOCH).build());
        assertEquals(3, downstreamConsumer.points.size());

        try {
            sorter.accept(Point.builder().time(EPOCH.minusSeconds(1)).build());
            //if no exception is thrown above then we should fail
            fail("We expect this point the get evicted -- and trigger the TimeOrderVerifyingConsumer");
        } catch (AssertionError ae) {
            //suppress the expected exception
        }
    }
}
