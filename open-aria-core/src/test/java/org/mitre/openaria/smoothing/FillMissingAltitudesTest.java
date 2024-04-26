
package org.mitre.openaria.smoothing;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.Distance;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;

import org.junit.jupiter.api.Test;

public class FillMissingAltitudesTest {

    @Test
    public void removeTracksWithNoAltitudes() {
        MutableTrack testTrack = trackWithNoAltitudes();
        Optional<MutableTrack> cleanedTrack = (new FillMissingAltitudes()).clean(testTrack);

        assertTrue(!cleanedTrack.isPresent(), "A track with no altitude data should be removed");
    }

    @Test
    public void testFillingInitialAltitudes() {
        MutableTrack testTrack = trackWithNoInitialAltitudes();
        MutableTrack cleanedTrack = (new FillMissingAltitudes()).clean(testTrack).get();
        ArrayList<Point> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            points.get(0).altitude().equals(points.get(1).altitude()) &&
                points.get(1).altitude().equals(points.get(2).altitude()),
            "The first points should have their altitudes filled"
        );
    }

    @Test
    public void testFillingFinalAltitudes() {
        MutableTrack testTrack = trackWithNoFinalAltitudes();
        MutableTrack cleanedTrack = (new FillMissingAltitudes()).clean(testTrack).get();
        ArrayList<Point> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            points.get(3).altitude().equals(points.get(1).altitude()) &&
                points.get(2).altitude().equals(points.get(1).altitude()),
            "The last points should have their altitudes filled"
        );
    }

    @Test
    public void testFillingMissingAltitude() {
        MutableTrack testTrack = trackWithSingleMissingAltitude();
        MutableTrack cleanedTrack = (new FillMissingAltitudes()).clean(testTrack).get();
        ArrayList<Point> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            points.get(1).altitude().inFeet() == 130.0,
            "The middle point's altitude should be filled based on its neighbors"
        );
    }

    @Test
    public void testFillingMultipleMissingAltitudes() {
        MutableTrack testTrack = trackWithMultipleMissingAltitudes();
        MutableTrack cleanedTrack = (new FillMissingAltitudes()).clean(testTrack).get();
        ArrayList<Point> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            (points.get(1).altitude().inFeet() == 0.0) &&
                (points.get(2).altitude().inFeet() == 20.0),
            "The middle points' altitudes should be filled based on their neighbors"
        );
    }

    private MutableTrack trackWithNoAltitudes() {
        return MutableTrack.of(new TreeSet<>(Arrays.asList(
            makeNullAltitudePoint(0),
            makeNullAltitudePoint(1),
            makeNullAltitudePoint(2)
        )));
    }

    private MutableTrack trackWithNoInitialAltitudes() {
        return MutableTrack.of(new TreeSet<>(Arrays.asList(
            makeNullAltitudePoint(0),
            makeNullAltitudePoint(1),
            makePoint(2, 100.0)
        )));
    }

    private MutableTrack trackWithNoFinalAltitudes() {
        return MutableTrack.of(new TreeSet<>(Arrays.asList(
            makePoint(0, 100.0),
            makePoint(1, 110.0),
            makeNullAltitudePoint(2),
            makeNullAltitudePoint(3)
        )));
    }

    private MutableTrack trackWithSingleMissingAltitude() {
        return MutableTrack.of(new TreeSet<>(Arrays.asList(
            makePoint(0, 100.0),
            makeNullAltitudePoint(3),
            makePoint(10, 200.0)
        )));
    }

    private MutableTrack trackWithMultipleMissingAltitudes() {
        return MutableTrack.of(new TreeSet<>(Arrays.asList(
            makePoint(0, -100.0),
            makeNullAltitudePoint(25),
            makeNullAltitudePoint(30),
            makePoint(100, 300.0)
        )));
    }

    private Point makePoint(int secondsFromStart, double altitudeInFeet) {
        return Point.builder()
            .time(EPOCH.plusSeconds(secondsFromStart))
            .altitude(Distance.ofFeet(altitudeInFeet))
            .buildMutable();
    }

    private Point makeNullAltitudePoint(int secondsFromStart) {
        return Point.builder()
            .time(EPOCH.plusSeconds(secondsFromStart))
            .buildMutable();
    }
}