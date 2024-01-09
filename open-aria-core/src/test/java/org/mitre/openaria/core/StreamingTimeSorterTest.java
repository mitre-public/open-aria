
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;

import com.google.common.collect.Lists;

public class StreamingTimeSorterTest {

    /*
     * This list of partially sorted data can (A) be sorted properly on demand or (B) not get sorted
     * by a TimeBasedSorter if the "alloted lag" is too small.
     */
    private static List<Point> testData() {

        ArrayList<Instant> times = Lists.newArrayList(
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(1),
            Instant.EPOCH.plusSeconds(10),
            Instant.EPOCH.plusSeconds(5),
            Instant.EPOCH.plusSeconds(11),
            Instant.EPOCH.plusSeconds(12),
            Instant.EPOCH.plusSeconds(13),
            Instant.EPOCH.plusSeconds(23)
        );

        ArrayList<Point> points = new ArrayList<>();
        for (Instant aTime : times) {
            points.add(new PointBuilder().time(aTime).build());
        }
        return points;
    }

    @Test
    public void testEmittedPointsAreProperlySorted() {

        Duration maxLag = Duration.ofSeconds(10L); //with this lag things get sorted correctly

        ConsumingArrayList<Point> consumer = newConsumingArrayList();

        StreamingTimeSorter sorter = new StreamingTimeSorter(consumer, maxLag);

        for (Point commonPoint : testData()) {
            sorter.accept(commonPoint);
        }

        assertThat(consumer, hasSize(6));

        verifyPointWereOrdered(consumer);
    }

    @Test
    public void testEmittedPointsAreNotProperlySorted() {

        //with this lag things do not get sorted correctly
        Duration maxLag = Duration.ofSeconds(2L);

        ConsumingArrayList<Point> downstreamConsumer = newConsumingArrayList();

        ConsumingArrayList<Point> rejectedPoints = newConsumingArrayList();

        StreamingTimeSorter sorter = new StreamingTimeSorter(downstreamConsumer, maxLag, rejectedPoints);

        //nothing emitted after 1st insert
        sorter.accept(new PointBuilder().time(EPOCH).build());
        assertEquals(downstreamConsumer.size(), 0);
        assertEquals(sorter.integritySummarizer().totalInputCount(), 1); //but the auditor knows it exists

        //nothing emitted after 2nd insert
        sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(1)).build());
        assertEquals(downstreamConsumer.size(), 0);
        assertEquals(sorter.integritySummarizer().totalInputCount(), 2); //but the auditor knows it exists

        //2 points emitted after 3rd insert
        sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(10)).build());
        assertEquals(downstreamConsumer.size(), 2);
        assertTrue(downstreamConsumer.get(0).time().equals(Instant.EPOCH));
        assertTrue(downstreamConsumer.get(1).time().equals(Instant.EPOCH.plusSeconds(1)));

        //3rd point emitted after 4th insert
        sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(5)).build());
        assertEquals(downstreamConsumer.size(), 3);
        assertTrue(downstreamConsumer.get(2).time().equals(Instant.EPOCH.plusSeconds(5)));

        //4th point emitted is and an out-of-time-order Point is sent to the "rejection consumer"
        assertThat(rejectedPoints.size(), is(0));
        sorter.accept(new PointBuilder().time(EPOCH.plusSeconds(4)).build()); //should throw exception here
        assertEquals(downstreamConsumer.size(), 3);
        assertThat(rejectedPoints.size(), is(1));
        assertThat(sorter.integritySummarizer().droppedCount(), is(1L)); //the auditor knows the point was dropped

        verifyPointWereOrdered(downstreamConsumer);
    }

    private static void verifyPointWereOrdered(ArrayList<Point> aggregator) {

        Point lastPoint = null;

        for (Point point : aggregator) {
            if (lastPoint == null) {
                continue;
            } else {
                assertTrue(lastPoint.time().isBefore(point.time()));
            }
            lastPoint = point;
        }
    }
}
