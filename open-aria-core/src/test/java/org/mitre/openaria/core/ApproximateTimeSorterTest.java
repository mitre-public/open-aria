
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;
import org.mitre.caasd.commons.HasTime;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

public class ApproximateTimeSorterTest {

    // This simple record allows us to test ApproximateTimeSorter
    record TimePojo(Instant time) implements HasTime {}

    /*
     * This list of partially sorted data can (A) be sorted properly on demand or (B) not get sorted
     * by a TimeBasedSorter if the "alloted lag" is too small.
     */
    private static List<TimePojo> testData() {

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

        return times.stream()
            .map(time -> new TimePojo(time))
            .toList();
    }

    @Test
    public void testAllPointsWithinWindow() {
        /*
         * Confirm that no points are emitted when all the points occur within the time window
         */
        Duration maxLag = Duration.ofMinutes(5);

        ApproximateTimeSorter<TimePojo> sorter = new ApproximateTimeSorter<>(
            maxLag,
            (TimePojo t) -> {
                throw new UnsupportedOperationException("No Point should be forward to this Consumer");
            }
        );

        for (TimePojo timeBox : testData()) {
            sorter.accept(timeBox);
        }
    }

    /**
     * This consumer verifies that CommonPoints passed to it are received in time-sorted order
     */
    static class TimeOrderVerifyingConsumer implements Consumer<TimePojo> {

        LinkedList<TimePojo> timePojos = new LinkedList<>();

        @Override
        public void accept(TimePojo t) {

            if (!timePojos.isEmpty()) {
                TimePojo lastPoint = timePojos.getLast();
                assertTrue(
                    lastPoint.time().isBefore(t.time())
                );
            }

            timePojos.addLast(t);
        }

        public int size() {
            return timePojos.size();
        }

        public Instant timeFor(int i) {
            return timePojos.get(i).time();
        }
    }

    @Test
    public void testEmittedPointsAreProperlySorted() {

        Duration maxLag = Duration.ofSeconds(10L); //with this lag things get sorted correctly

        TimeOrderVerifyingConsumer consumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter<TimePojo> sorter = new ApproximateTimeSorter<>(
            maxLag,
            consumer
        );

        for (TimePojo timeBox : testData()) {
            sorter.accept(timeBox);
        }

        assertEquals(7, consumer.size());
    }

    @Test
    public void testEmittedPointsAreNotProperlySorted() {

        //with this lag things do not get sorted correctly
        Duration maxLag = Duration.ofSeconds(2L);

        TimeOrderVerifyingConsumer consumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter<TimePojo> sorter = new ApproximateTimeSorter<>(maxLag, consumer);

        sorter.accept(new TimePojo(EPOCH));
        assertEquals(0, consumer.size());

        sorter.accept(new TimePojo(EPOCH.plusSeconds(1)));
        assertEquals(0, consumer.size());

        sorter.accept(new TimePojo(EPOCH.plusSeconds(10)));
        assertEquals(2, consumer.size());
        assertEquals(consumer.timeFor(0), EPOCH);
        assertEquals(consumer.timeFor(1), EPOCH.plusSeconds(1));

        sorter.accept(new TimePojo(EPOCH.plusSeconds(5)));
        assertEquals(3, consumer.size());
        assertEquals(consumer.timeFor(2), EPOCH.plusSeconds(5));

        //this TestConsumer fails here because this point is passed through the sorter (after the 5 point was)
        assertThrows(
            AssertionError.class,
            () -> sorter.accept(new TimePojo(EPOCH.plusSeconds(4)))
        );
    }

    @Test
    public void testFlush() {
        /**
         * Confirm all points are emitted in the proper order upon calling "flush"
         */
        Duration maxLag = Duration.ofMinutes(5);

        TimeOrderVerifyingConsumer consumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter<TimePojo> sorter = new ApproximateTimeSorter<>(
            maxLag,
            consumer
        );

        for (TimePojo commonPoint : testData()) {
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

        Duration maxLag = Duration.ofSeconds(3);  //a very small sorting window

        ConsumingArrayList<TimePojo> downstreamConsumer = newConsumingArrayList();

        ApproximateTimeSorter<TimePojo> sorter = new ApproximateTimeSorter<>(maxLag, downstreamConsumer);

        //add Points to the sorter that keep getting older and older
        int NUM_POINTS = 50_000;
        for (int i = 0; i < NUM_POINTS; i++) {
            Instant time = EPOCH.minusSeconds(i);
            sorter.accept(new TimePojo(time));
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

        ApproximateTimeSorter<Point> sorter = new ApproximateTimeSorter<>(
            Duration.ofSeconds(10),
            downstreamConsumer
        );

        sorter.accept(Point.builder().latLong(0.0,0.0).time(EPOCH).build());
        sorter.accept(Point.builder().latLong(0.0,0.0).time(EPOCH.plusSeconds(4)).build());
        sorter.accept(Point.builder().latLong(0.0,0.0).time(EPOCH.plusSeconds(8)).build());
        assertThat("no evictions yet", downstreamConsumer, empty());

        //Adding this point evicts the 1st point because it is more than 10 seconds older than the most recent input
        sorter.accept(Point.builder().latLong(0.0,0.0).time(EPOCH.plusSeconds(12)).build()); //evict Epoch + 0
        assertThat(downstreamConsumer, hasSize(1));
        assertThat(downstreamConsumer.get(0).time(), equalTo(EPOCH));

        //This stale point should get instantly evicted because it is too old (with respect to time highwater mark)
        sorter.accept(Point.builder().latLong(0.0,0.0).time(EPOCH.minusSeconds(20)).build());
        assertThat(downstreamConsumer, hasSize(2));
        assertThat(downstreamConsumer.get(1).time(), equalTo(EPOCH.minusSeconds(20)));
    }

    @Test
    public void testStalePointsAreNotAutoEvicted() {

        TimeOrderVerifyingConsumer downstreamConsumer = new TimeOrderVerifyingConsumer();

        ApproximateTimeSorter<TimePojo> sorter = new ApproximateTimeSorter<>(
            Duration.ofSeconds(10),
            downstreamConsumer
        );

        sorter.accept(new TimePojo(EPOCH));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(4)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(8)));
        assertEquals(0, downstreamConsumer.timePojos.size());

        //Adding this point should evict the 1st point because it is more than 10 seconds older than the most recent input
        sorter.accept(new TimePojo(EPOCH.plusSeconds(12))); //evict Epoch + 0
        assertEquals(1, downstreamConsumer.timePojos.size());
        sorter.accept(new TimePojo(EPOCH.plusSeconds(16))); //evict Epoch + 4
        sorter.accept(new TimePojo(EPOCH.plusSeconds(20))); //evict Epoch + 8
        assertEquals(3, downstreamConsumer.timePojos.size());

        //none of these "semi-stale" points are auto evicted because are in the "wait for more data" window
        sorter.accept(new TimePojo(EPOCH.plusSeconds(14)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(13)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(12)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(11)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(10)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(9)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(8)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(7)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(6)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(5)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(4)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(3)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(2)));
        sorter.accept(new TimePojo(EPOCH.plusSeconds(1)));
        sorter.accept(new TimePojo(EPOCH));
        assertEquals(3, downstreamConsumer.timePojos.size());

        try {
            sorter.accept(new TimePojo(EPOCH.minusSeconds(1)));
            //if no exception is thrown above then we should fail
            fail("We expect this point the get evicted -- and trigger the TimeOrderVerifyingConsumer");
        } catch (AssertionError ae) {
            //suppress the expected exception
        }
    }
}
