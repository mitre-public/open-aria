
package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingLinkedList;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.mitre.caasd.commons.ConsumingCollections.ConsumingLinkedList;
import org.mitre.caasd.commons.fileutil.FileUtils;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StrictTimeSortEnforcerTest {

    private static final String WARNING_DIR = "warningDir";

    private static File temporaryTestDirectory;

    @BeforeAll
    public static void setup() {
        //these tests create Warning Files...put them in a temporary location
        temporaryTestDirectory = new File(WARNING_DIR);
        if (!temporaryTestDirectory.exists()) {
            temporaryTestDirectory.mkdir();
        }
    }

    @AfterAll
    public static void teardown() throws Exception {
        //clean up all Warning Files
        if (temporaryTestDirectory.exists()) {
            FileUtils.deleteDirectory(temporaryTestDirectory);
        }
    }

    /**
     * This consumer verifies that CommonPoints passed to it are received in time-sorted order
     */
    class TestConsumer implements Consumer<Point> {

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
    }

    /*
     * Create a List of Points with times shifted different numbers of seconds from EPOCH.
     */
    private static List<Point> testData(int... seconds) {

        ArrayList<Instant> times = Lists.newArrayList();
        for (int second : seconds) {
            times.add(Instant.EPOCH.plusSeconds(second));
        }

        ArrayList<Point> points = new ArrayList<>();
        for (Instant aTime : times) {
            points.add(new PointBuilder().time(aTime).latLong(0.0, 0.0).build());
        }
        return points;
    }

    /*
     * Test: (1) points in the past generate warnings (3) Points in the past are "published" via the
     * "lastSkippedPoint()"
     */
    @Test
    public void testTestConsumerFailsOnBadInput() {

        List<Point> testData = testData(0, 1, -1);

        TestConsumer testConsumer = new TestConsumer();

        try {
            for (Point point : testData) {
                testConsumer.accept(point);
            }
            fail("the 3rd point should cause an exception");
        } catch (AssertionError ae) {
            //properly caught the exception (other would have failed)
        }
    }

    @Test
    public void testPointsActuallyDeliveredAreOrdered() {

        TestConsumer testConsumer = new TestConsumer();

        StrictTimeSortEnforcer sorted = new StrictTimeSortEnforcer(testConsumer);

        //only points with indice 0, 1, 3, 5, 6 should be delivered
        List<Point> testData = testData(0, 1, -1, 2, -3, 3, 4, -1, 3);

        for (Point point : testData) {
            sorted.accept(point);
        }

        assertEquals(
            5, testConsumer.points.size(),
            "Exactly 5 points should have been delivered"
        );
    }

    @Test
    public void testSkippedAreAvailableInLastSkippedPoint() {

        TestConsumer testConsumer = new TestConsumer();
        ConsumingLinkedList rejectionConsumer = newConsumingLinkedList();

        StrictTimeSortEnforcer sorted = new StrictTimeSortEnforcer(
            testConsumer,
            rejectionConsumer
        );

        //only points with indice 0, 1, 3, 5, 6 should be delivered
        List<Point> testData = testData(0, 1, -1, 2, -3, 3, 4, -1, 3);

        sorted.accept(testData.get(0));
        sorted.accept(testData.get(1));
        sorted.accept(testData.get(2)); //this point should be skipped

        assertEquals(rejectionConsumer.getLast(), testData.get(2));

        sorted.accept(testData.get(3));
        sorted.accept(testData.get(4));  //this point should be skipped

        assertEquals(rejectionConsumer.getLast(), testData.get(4));

        sorted.accept(testData.get(5));
        sorted.accept(testData.get(6));
        sorted.accept(testData.get(7));  //this point should be skipped

        assertEquals(rejectionConsumer.getLast(), testData.get(7));

        sorted.accept(testData.get(8));  //this point should be skipped

        assertEquals(rejectionConsumer.getLast(), testData.get(8));
    }
}
