package org.mitre.openaria.core.data;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static java.time.Instant.EPOCH;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.shuffle;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.parsing.nop.NopParsingUtils.parseNopTime;
import static org.mitre.openaria.core.data.Points.*;
import static org.mitre.openaria.core.data.Tracks.createTrackFromCsvFile;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.mitre.caasd.commons.TimeWindow;

import org.junit.jupiter.api.Test;

class PointsTest {


    @Test
    public void testCompareTo() {
        /*
         * Confirm that when two different Points are added to a TreeSet one does not evict the
         * other just because the Points have the same time.
         *
         * This is not a direct test of compareTo. However, this test emphasizes that we want
         * compareTo to enable standard data-structures to work intuitavely.
         */

        Point<?> p1 = Point.builder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .build();

        Point<?> p2 = Point.builder()
            .latLong(1.0, 1.0)
            .time(Instant.EPOCH)
            .build();

        assertNotEquals(p1, p2, "The two test points are not the same");

        TreeSet<Point<?>> tree1 = new TreeSet<>();
        tree1.add(p1);
        tree1.add(p2);
        assertThat("A TreeSet should be able to differentiate these points", tree1.size(), is(2));
        assertThat("Point p1 is the lessor point because its longitude is 0", p1.compareTo(p2), is(-1));
    }

    @Test
    public void testNullableComparator() {

        assertEquals(-1, Points.NULLABLE_COMPARATOR.compare(null, "b"));
        assertEquals(-1, Points.NULLABLE_COMPARATOR.compare("a", "b"));
        assertEquals(0, Points.NULLABLE_COMPARATOR.compare("a", "a"));
        assertEquals(1, Points.NULLABLE_COMPARATOR.compare("b", "a"));
        assertEquals(1, Points.NULLABLE_COMPARATOR.compare("b", null));

        assertEquals(0, Points.NULLABLE_COMPARATOR.compare(null, null));
    }

    @Test
    public void testKNearestPoints() throws Exception {

        File file = new File("src/test/resources/csvTrack1.txt");

        if (file == null) {
            fail();
        }

        try {
            //read a File, convert each Line to a Point and collect the results in a list
            List<CsvPoint> points = Files.lines(file.toPath())
                .map(s -> new CsvPoint(s))
                .collect(toList());

            shuffle(points);  //requires mutable list

            NavigableSet<Point> kNN = slowKNearestPoints(
                points,
                parseNopTime("07/08/2017", "14:11:59.454"),
                5
            );

            confirmCsvEquality(
                kNN,
                ",,2017-07-08T14:11:50.254Z,GEG-655,47.5884,-117.6554,3800,W1JIXSxTVEFSUyxHRUcsMDcvMDgvMjAxNywxNDoxMTo1MC4yNTQsLCwsMTIwMCwzOCwxMzIsMjY0LDQ3LjU4ODQsLTExNy42NTUzNSw2NTUsMCwtNC4yNDYsLTEuODc4OSwsLCxHRUcsLCwsLCwsSUZSLCwsLCwsLCwsLCwse1JIfQ",
                ",,2017-07-08T14:11:55.044Z,GEG-655,47.5876,-117.6595,3900,W1JIXSxTVEFSUyxHRUcsMDcvMDgvMjAxNywxNDoxMTo1NS4wNDQsLCwsMTIwMCwzOSwxMzQsMjUwLDQ3LjU4NzU1LC0xMTcuNjU5NDgsNjU1LDAsLTQuNDE0MSwtMS45Mjk3LCwsLEdFRywsLCwsLCxJRlIsLCwsLCwsLCwsLCx7Ukh9",
                ",,2017-07-08T14:11:59.454Z,GEG-655,47.5869,-117.6635,3900,W1JIXSxTVEFSUyxHRUcsMDcvMDgvMjAxNywxNDoxMTo1OS40NTQsLCwsMTIwMCwzOSwxMzIsMjU3LDQ3LjU4Njg5LC0xMTcuNjYzNTIsNjU1LDAsLTQuNTc4MiwtMS45Njg4LCwsLEdFRywsLCwsLCxJRlIsLCwsLCwsLCwsLCx7Ukh9",
                ",,2017-07-08T14:12:04.134Z,GEG-655,47.5859,-117.6674,3900,W1JIXSxTVEFSUyxHRUcsMDcvMDgvMjAxNywxNDoxMjowNC4xMzQsLCwsMTIwMCwzOSwxMzMsMjQ2LDQ3LjU4NTg1LC0xMTcuNjY3MzYsNjU1LDAsLTQuNzM0MywtMi4wMzEzLCwsLEdFRywsLCwsLCxJRlIsLCwsLCwsLCwsLCx7Ukh9",
                ",,2017-07-08T14:12:08.734Z,GEG-655,47.5845,-117.6710,3900,W1JIXSxTVEFSUyxHRUcsMDcvMDgvMjAxNywxNDoxMjowOC43MzQsLCwsMTIwMCwzOSwxMzMsMjQ4LDQ3LjU4NDQ4LC0xMTcuNjcxMDIsNjU1LDAsLTQuODgyOCwtMi4xMTMyLCwsLEdFRywsLCwsLCxJRlIsLCwsLCwsLCwsLCx7Ukh9"
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            fail("exception occured " + ex.getMessage());
        }
    }

    @Test
    public void testKNearestPointsWithGenericInputs() {
        /*
         * This test confirms that the signature of kNearestPoints will accept any Collection<T>
         * where T is some arbitrary class that implements Point
         */
        List<BasicPointImplementation> listOfPoints = newArrayList();

        NavigableSet<Point> neighbors = slowKNearestPoints(listOfPoints, Instant.EPOCH, 2);

        assertTrue(
            neighbors.isEmpty(),
            "There should be no neighbors because the input list was empty"
        );
    }

    static final Point p1 = newPoint(EPOCH);
    static final Point p2 = newPoint(EPOCH, "D10-1234"); //adding a beacon code makes it distinct from p1
    static final Point p3 = newPoint(EPOCH.plusSeconds(1));
    static final Point p4 = newPoint(EPOCH.plusSeconds(1), "D10-1234"); //adding a linkId makes it distinct from p3
    static final Point p5 = newPoint(EPOCH.plusSeconds(2));
    static final Point p6 = newPoint(EPOCH.plusSeconds(3));
    static final Point p7 = newPoint(EPOCH.plusSeconds(4));
    static final Point p8 = newPoint(EPOCH.plusSeconds(5));
    static final Point p9 = newPoint(EPOCH.plusSeconds(6));
    static final Point p10 = newPoint(EPOCH.plusSeconds(7));
    static final Point p11 = newPoint(EPOCH.plusSeconds(8));
    static final Point p12 = newPoint(EPOCH.plusSeconds(9));

    static final TreeSet<Point> points = newTreeSet(
        newArrayList(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)
    );

    @Test
    public void testFastKNearestPoints_1() {
        NavigableSet<Point> knn = fastKNearestPoints(points, EPOCH.plusSeconds(20), 1);
        assertEquals(1, knn.size());
        Point neighbor = knn.pollFirst();
        assertTrue(neighbor == p12);
        assertEquals(neighbor.time(), EPOCH.plusSeconds(9));
    }

    @Test
    public void testFastKNearestPoints_2() {
        NavigableSet<Point> knn = fastKNearestPoints(points, EPOCH, 2);
        assertEquals(2, knn.size());
        Point one = knn.pollFirst();
        Point two = knn.pollFirst();
        assertFalse(one == two, "This objects are different");
        assertEquals(one.time(), EPOCH, "Both match the search time");
        assertEquals(two.time(), EPOCH, "Both match the search time");
    }

    @Test
    public void testFastKNearestPoints_3() {
        NavigableSet<Point> knn = fastKNearestPoints(points, EPOCH.plusMillis(5), 3);
        assertEquals(3, knn.size());
        Point one = knn.pollFirst();
        Point two = knn.pollFirst();
        Point three = knn.pollFirst();
        assertFalse(one == two, "This objects are different");
        assertEquals(one.time(), EPOCH, "Both match the search time");
        assertEquals(two.time(), EPOCH, "Both match the search time");
        assertTrue(three == p3);
        assertEquals(three.time(), EPOCH.plusSeconds(1));
    }

    @Test
    public void testFastKNearestPoints_4() {
        //Searching for a "time" that is NOT used in the points dataset works
        NavigableSet<Point> knn = fastKNearestPoints(points, EPOCH.plusMillis(5_123), 3);
        assertEquals(3, knn.size());
        Point one = knn.pollFirst();
        Point two = knn.pollFirst();
        Point three = knn.pollFirst();
        //note: the neighbors are in time order, not "closest to search time" order
        assertTrue(one == p7);
        assertTrue(two == p8);
        assertTrue(three == p9);
    }

    @Test
    public void testFastKNearestPoints_5() {
        //Searching for a "time" that is used in the points dataset works
        NavigableSet<Point> knn = fastKNearestPoints(points, EPOCH.plusSeconds(5), 3);
        assertEquals(3, knn.size());
        Point one = knn.pollFirst();
        Point two = knn.pollFirst();
        Point three = knn.pollFirst();
        //note: the neighbors are in time order, not "closest to search time" order
        assertTrue(one == p7);
        assertTrue(two == p8);
        assertTrue(three == p9);
    }

    @Test
    public void testFastKNearestPoints_6() {
        NavigableSet<Point> knn = fastKNearestPoints(points, EPOCH.plusSeconds(5), 100);
        assertEquals(points.size(), knn.size());
        assertFalse(knn == points, "The datasets are different");
        knn.pollFirst();
        assertEquals(points.size(), knn.size() + 1, "We removed an item from the knn");
    }

    @Test
    public void testFastKNearestPoints_7() {
        NavigableSet<Point> knn = fastKNearestPoints(points, EPOCH.plusSeconds(5), 0);
        assertEquals(0, knn.size());
        assertEquals(12, points.size());
    }

    private static Point<?> newPoint(Instant time) {
        return PointRecord.builder().latLong(0.0, 0.0).time(time).build();
    }

    private static Point<?> newPoint(Instant time, String linkId) {
        return PointRecord.builder().latLong(0.0, 0.0).time(time).linkId(linkId).build();
    }

    @Test
    public void subset_returnsEmptyCollectionWhenNothingQualifies() {

        Track t1 = createTrackFromCsvFile(new File("src/test/resources/csvTrack1.txt"));

        TimeWindow windowThatDoesNotOverlapWithTrack = TimeWindow.of(EPOCH, EPOCH.plusSeconds(100));

        TreeSet<Point> subset = subset(windowThatDoesNotOverlapWithTrack, (NavigableSet<Point>) t1.points());

        assertThat(subset, hasSize(0));
    }

    @Test
    public void subset_reflectsEndTime() {

        Track t1 = createTrackFromCsvFile(new File("src/test/resources/csvTrack1.txt"));

        //this is the time of 21st point in the track
        Instant endTime = parseNopTime("07/08/2017", "14:10:45.534");

        TimeWindow extractionWindow = TimeWindow.of(EPOCH, endTime);

        TreeSet<Point> subset = subset(extractionWindow, (NavigableSet<Point>) t1.points());

        assertThat(subset, hasSize(21));
        assertThat(subset.last().time(), is(endTime));
    }

    @Test
    public void subset_reflectsStartTime() {

        Track t1 = createTrackFromCsvFile(new File("src/test/resources/csvTrack1.txt"));

        //this is the time of 21st point in the track
        Instant startTime = parseNopTime("07/08/2017", "14:10:45.534");

        TimeWindow extractionWindow = TimeWindow.of(startTime, startTime.plus(365 * 20, DAYS));

        NavigableSet<Point> subset = (NavigableSet<Point>) subset(extractionWindow, (NavigableSet<Point>) t1.points());

        assertThat(subset, hasSize(t1.size() - 21 + 1)); //"+1" because the fence post Point is in both the original track and the subset

        //the first point in the subset has the correct time
        assertThat(subset.first().time(), is(startTime));
    }

    @Test
    public void subset_reflectsStartAndEndTimes() {

        Track t1 = createTrackFromCsvFile(new File("src/test/resources/csvTrack1.txt"));

        Instant startTime = parseNopTime("07/08/2017", "14:10:45.534");
        Instant endTime = parseNopTime("07/08/2017", "14:11:17.854");
        TimeWindow extractionWindow = TimeWindow.of(startTime, endTime);

        TreeSet<Point> subset = subset(extractionWindow, (NavigableSet<Point>) t1.points());

        assertThat(subset, hasSize(8));

        assertThat(subset.first().time(), is(startTime));
        assertThat(subset.last().time(), is(endTime));
    }


    /**
     * Confirm (1) that the number Points in the actualResult equals the number of Strings in the
     * expected points array, and (2) that every Point in the actual results corresponds to a String
     * in the expected Points array (using p.asCsvText()).
     * <p>
     * This method is used to write unit tests that verify an expected outcome set.
     *
     * @param actualResults  A Collection of points (typically generated algorithmically)
     * @param expectedPoints An array of NOP Strings
     */
    public static void confirmCsvEquality(Collection<Point> actualResults, String... expectedPoints) {

        assertEquals(
            actualResults.size(), expectedPoints.length,
            "The number of actual results must match the number of expected Points"
        );

        Set<String> arrayOfPoints = newHashSet(expectedPoints);

        for (Point actualResult : actualResults) {
            assertTrue(
                arrayOfPoints.contains(actualResult.asCsvText()),
                "The actualResult:\n" + actualResult.asCsvText() + " \nwas not found in the array of expected points"
            );
        }
    }

}



