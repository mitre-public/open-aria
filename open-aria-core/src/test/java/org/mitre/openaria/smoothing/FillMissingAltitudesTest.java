
package org.mitre.openaria.smoothing;

import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.Distance;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class FillMissingAltitudesTest {

    record NoRawData(String doNotUse) {}

    @Test
    public void removeTracksWithNoAltitudes() {
        Track<NoRawData> testTrack = trackWithNoAltitudes();
        Optional<Track<NoRawData>> cleanedTrack = (new FillMissingAltitudes<NoRawData>()).clean(testTrack);

        assertTrue(!cleanedTrack.isPresent(), "A track with no altitude data should be removed");
    }

    @Test
    public void testFillingInitialAltitudes() {
        Track<NoRawData> testTrack = trackWithNoInitialAltitudes();
        Track<NoRawData> cleanedTrack = (new FillMissingAltitudes<NoRawData>()).clean(testTrack).get();
        ArrayList<Point<NoRawData>> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            points.get(0).altitude().equals(points.get(1).altitude()) &&
                points.get(1).altitude().equals(points.get(2).altitude()),
            "The first points should have their altitudes filled"
        );
    }

    @Test
    public void testFillingFinalAltitudes() {
        Track<NoRawData> testTrack = trackWithNoFinalAltitudes();
        Track<NoRawData> cleanedTrack = (new FillMissingAltitudes<NoRawData>()).clean(testTrack).get();
        ArrayList<Point<NoRawData>> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            points.get(3).altitude().equals(points.get(1).altitude()) &&
                points.get(2).altitude().equals(points.get(1).altitude()),
            "The last points should have their altitudes filled"
        );
    }

    @Test
    public void testFillingMissingAltitude() {
        Track<NoRawData> testTrack = trackWithSingleMissingAltitude();
        Track<NoRawData> cleanedTrack = (new FillMissingAltitudes<NoRawData>()).clean(testTrack).get();
        ArrayList<Point<NoRawData>> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            points.get(1).altitude().inFeet() == 130.0,
            "The middle point's altitude should be filled based on its neighbors"
        );
    }

    @Test
    public void testFillingMultipleMissingAltitudes() {
        Track<NoRawData> testTrack = trackWithMultipleMissingAltitudes();
        Track<NoRawData> cleanedTrack = (new FillMissingAltitudes<NoRawData>()).clean(testTrack).get();
        ArrayList<Point<NoRawData>> points = new ArrayList<>(cleanedTrack.points());

        assertTrue(
            (points.get(1).altitude().inFeet() == 0.0) &&
                (points.get(2).altitude().inFeet() == 20.0),
            "The middle points' altitudes should be filled based on their neighbors"
        );
    }

    private Track<NoRawData> trackWithNoAltitudes() {
        return Track.of(new TreeSet<>(Arrays.asList(
            makeNullAltitudePoint(0),
            makeNullAltitudePoint(1),
            makeNullAltitudePoint(2)
        )));
    }

    private Track<NoRawData> trackWithNoInitialAltitudes() {
        return Track.of(new TreeSet<>(Arrays.asList(
            makeNullAltitudePoint(0),
            makeNullAltitudePoint(1),
            makePoint(2, 100.0)
        )));
    }

    private Track<NoRawData> trackWithNoFinalAltitudes() {
        return Track.of(new TreeSet<>(Arrays.asList(
            makePoint(0, 100.0),
            makePoint(1, 110.0),
            makeNullAltitudePoint(2),
            makeNullAltitudePoint(3)
        )));
    }

    private Track<NoRawData> trackWithSingleMissingAltitude() {
        return Track.of(new TreeSet<>(Arrays.asList(
            makePoint(0, 100.0),
            makeNullAltitudePoint(3),
            makePoint(10, 200.0)
        )));
    }

    private Track<NoRawData> trackWithMultipleMissingAltitudes() {
        return Track.of(new TreeSet<>(Arrays.asList(
            makePoint(0, -100.0),
            makeNullAltitudePoint(25),
            makeNullAltitudePoint(30),
            makePoint(100, 300.0)
        )));
    }

    private Point<NoRawData> makePoint(int secondsFromStart, double altitudeInFeet) {
        return Point.<NoRawData>builder()
            .time(EPOCH.plusSeconds(secondsFromStart))
            .altitude(Distance.ofFeet(altitudeInFeet))
            .latLong(0.0, 0.0)
            .build();
    }

    private Point<NoRawData> makeNullAltitudePoint(int secondsFromStart) {
        return Point.<NoRawData>builder()
            .time(EPOCH.plusSeconds(secondsFromStart))
            .latLong(0.0, 0.0)
            .build();
    }
}