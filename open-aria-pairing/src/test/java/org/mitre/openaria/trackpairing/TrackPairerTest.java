
package org.mitre.openaria.trackpairing;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;
import static org.mitre.openaria.core.Point.builder;
import static org.mitre.openaria.pointpairing.PairingConfig.standardPairingProperties;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.function.Predicate;

import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;
import org.mitre.caasd.commons.Pair;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.nop.NopParser;
import org.mitre.openaria.threading.TrackMaker;

import org.junit.jupiter.api.Test;

public class TrackPairerTest {

    /**
     * This files contains two tracks worth of NOP data. These tracks come EXTREMELY close to each
     * other.
     */
    private static String TEST_FILE = "twoTracks.txt";

    private static File getTestFile() {
        File file = new File("src/test/resources/twoTracks.txt");
//        Optional<File> file = FileUtils.getResourceAsFile(TrackPairer.class, TEST_FILE);

        if (!file.exists()) {
            fail("could not find test file: " + TEST_FILE);
        }
        return file;
    }

    private static ArrayList<Point> getTestPoints() {
        PointIterator ptIter = new PointIterator(new NopParser(getTestFile()));
        return newArrayList(ptIter);
    }

    @Test
    public void confirmOneTrackPairIsEmitted() {
        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();
        ConsumingArrayList<TrackPair> pairConsumer = newConsumingArrayList();

        TrackPairer instance = new TrackPairer(
            trackConsumer,
            pairConsumer,
            standardPairingProperties()
        );

        ArrayList<Point> points = getTestPoints();
        Collections.sort(points);

        for (Point point : points) {
            instance.accept(point);
        }

        instance.innerTrackMaker().flushAllTracks();

        assertThat(trackConsumer, hasSize(2));
        assertThat(pairConsumer, hasSize(1));
    }

    @Test
    public void confirmTracksMatch() {

        ConsumingArrayList<Track> fromMaker = tracksFromTrackMaker();
        ConsumingArrayList<Track> fromPairer = tracksFromTrackPairer();

        assertEquals(fromMaker.size(), fromPairer.size());
        assertEquals(fromMaker.size(), 2);
        assertEquals(fromPairer.size(), 2);

        for (int i = 0; i < 2; i++) {
            confirmTracksMatch(
                fromMaker.get(i),
                fromPairer.get(i)
            );
        }
    }

    private static void confirmTracksMatch(Track t1, Track t2) {

        assertEquals(t1.size(), t2.size());
        NavigableSet<? extends Point> t1Points = newTreeSet(t1.points());
        NavigableSet<? extends Point> t2Points = newTreeSet(t2.points());
        while (!t1Points.isEmpty()) {
            Point t1Point = t1Points.pollFirst();
            Point t2Point = t2Points.pollFirst();
            assertEquals(t1Point.asNop(), t2Point.asNop());
        }
    }

    private static ConsumingArrayList<Track> tracksFromTrackMaker() {
        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();

        TrackMaker maker = new TrackMaker(trackConsumer);

        ArrayList<Point> points = getTestPoints();
        Collections.sort(points);

        for (Point point : points) {
            maker.accept(point);
        }
        maker.flushAllTracks();

        return trackConsumer;
    }

    private static ConsumingArrayList<Track> tracksFromTrackPairer() {
        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();
        ConsumingArrayList<TrackPair> pairConsumer = newConsumingArrayList();

        TrackPairer instance = new TrackPairer(
            trackConsumer,
            pairConsumer,
            standardPairingProperties()
        );

        ArrayList<Point> points = getTestPoints();
        Collections.sort(points);

        for (Point point : points) {
            instance.accept(point);
        }

        instance.innerTrackMaker().flushAllTracks();

        return trackConsumer;
    }

    @Test
    public void confirmFilteringWorks() {

        Predicate<Pair<Point, Point>> alwaysFalsePredicate = (pair) -> false;
        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();
        ConsumingArrayList<TrackPair> pairConsumer = newConsumingArrayList();

        TrackPairer instance = new TrackPairer(
            ConsumerPair.of(trackConsumer, pairConsumer),
            standardPairingProperties(),
            alwaysFalsePredicate
        );

        ArrayList<Point> points = getTestPoints();
        Collections.sort(points);

        for (Point point : points) {
            instance.accept(point);
        }

        instance.innerTrackMaker().flushAllTracks();

        //we got 2 tracks...
        assertEquals(2, trackConsumer.size());
        //but no pairs
        assertEquals(0, pairConsumer.size());
    }

    @Test
    public void confirmReusedTrackIdDoesNotHideEvent() {
        /*
         * A Bug was found in which an event was missed because the TrackId of one of the tracks in
         * a TrackPair was reused while the other track in the TrackPair was still in flight (i.e.
         * one short track got paired with one very long track...then the short track got
         * overwritten by a new track that had the same TrackID)
         */

        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();
        ConsumingArrayList<TrackPair> pairConsumer = newConsumingArrayList();

        TrackPairer instance = new TrackPairer(
            trackConsumer,
            pairConsumer,
            standardPairingProperties()
        );

        ArrayList<Point> points = getOverwrittenTrackPairPoints();
        Collections.sort(points);

        for (Point point : points) {
            instance.accept(point);
        }

        instance.innerTrackMaker().flushAllTracks();

        assertThat(trackConsumer, hasSize(3));
        assertThat(pairConsumer, hasSize(1));

        assertThat(pairConsumer.get(0).track1().callsign(), is("AAL1229"));
        assertThat(pairConsumer.get(0).track2().callsign(), is("SWA2208"));  //and not "NDU543"
    }

    private static ArrayList<Point> getOverwrittenTrackPairPoints() {

        File testFile = new File("src/test/resources/overwrittenTrackPair.txt");

        if (!testFile.exists()) {
            fail("could not find test file: " + TEST_FILE);
        }

        PointIterator ptIter = new PointIterator(new NopParser(testFile));
        return newArrayList(ptIter);
    }

    @Test
    public void confirmReusedTrackIdDoesNotHideEvent_2() {
        /*
         * A Bug was found in which an event was missed because the TrackId of one of the tracks in
         * a TrackPair was reused while the other track in the TrackPair was still in flight (i.e.
         * one short track got paired with one very long track...then the short track got
         * overwritten by a new track that had the same TrackID)
         */

        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();
        ConsumingArrayList<TrackPair> pairConsumer = newConsumingArrayList();

        TrackPairer instance = new TrackPairer(
            trackConsumer,
            pairConsumer,
            standardPairingProperties()
        );

        ArrayList<Point> points = getDataForEventThatWasMissed();
        Collections.sort(points);

        for (Point point : points) {
            instance.accept(point);
        }

        instance.innerTrackMaker().flushAllTracks();

        //there are 4 tracks, The trackId 2664 is used 3 times while 3508 is used once
        assertThat(trackConsumer, hasSize(4));
        assertThat(pairConsumer, hasSize(2));

        System.out.println(pairConsumer.get(0).track2());

        //in the bug this is the only event that was found...
        assertThat(pairConsumer.get(0).track1().callsign(), is("N80AB"));
        assertThat(pairConsumer.get(0).track2().callsign(), is("UNKNOWN"));

        //in the bug this event (which is serious) was not detected becuase the preceeding event was the only one caught
        assertThat(pairConsumer.get(1).track1().callsign(), is("N80AB"));
        assertThat(pairConsumer.get(1).track2().callsign(), is("N38CT"));
    }

    private static ArrayList<Point> getDataForEventThatWasMissed() {

//        File testFile = FileUtils.getResourceFile("missedPair.txt");
        File testFile = new File("src/test/resources/missedPair.txt");

        if (!testFile.exists()) {
            fail("could not find test file: " + TEST_FILE);
        }

        PointIterator ptIter = new PointIterator(new NopParser(testFile));
        return newArrayList(ptIter);
    }

    @Test
    public void confirmTwoTrackPairsAreEmitted() {

        ConsumingArrayList<Track> trackConsumer = newConsumingArrayList();
        ConsumingArrayList<TrackPair> pairConsumer = newConsumingArrayList();

        TrackPairer instance = new TrackPairer(
            trackConsumer,
            pairConsumer,
            standardPairingProperties()
        );

        ArrayList<Point> points = getPointsForTracksWithGap();
        Collections.sort(points);

        for (Point point : points) {
            instance.accept(point);
        }

        instance.innerTrackMaker().flushAllTracks();

        // this passes:
        assertThat("Should get 1 full track, 2 track segments", trackConsumer, hasSize(3));

        // this fails:
        assertThat("Should get 2 track pairs", pairConsumer, hasSize(2));
    }

    private ArrayList<Point> getPointsForTracksWithGap() {

        ArrayList<Point> allPoints = new ArrayList<>(200);
        Instant t0 = Instant.ofEpochSecond(123456);

        // this track has a 46 second gap (which exceeds the default 45 sec threshold)

        for (int i = 0; i < 20; i++) {

            Point<?> aPoint = builder()
                .time(t0.plusSeconds(i))
                .trackId("1234").latLong(38.897226, -77.063974).build();

            allPoints.add(aPoint);
        }
        for (int i = 65; i < 100; i++) {
            Point<?> aPoint = builder()
                .time(t0.plusSeconds(i))
                .trackId("1234").latLong(38.897226, -77.063974).build();

            allPoints.add(aPoint);
        }

        // this track exists at the same time as previous one, but has no gaps

        for (int i = 0; i < 100; i++) {

            Point<?> aPoint = builder()
                .time(t0.plusSeconds(i))
                .trackId("9876").latLong(38.856834, -77.022491).build();

            allPoints.add(aPoint);
        }

        return allPoints;
    }
}
