package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;
import static org.mitre.openaria.smoothing.TrackSmoothing.coreSmoothing;

import java.util.Optional;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class ZeroAltitudeFilterTest {

    @Test
    public void trackWithAllZeroAltitudesTest() {
        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "allPointsHaveZeroAltitude.txt");

        Track cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        Optional<Point> any = cleaned.points().stream().filter(mp -> !mp.altitudeIsMissing()).findAny();

        assertFalse(any.isPresent());
    }

    @Test
    public void trackThatStartsWithZeroAltitudesTest() {
        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "firstPointsHaveZeroAltitude.txt");

        Track cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        Point first = cleaned.points().first();

        assertTrue(first.altitudeIsMissing());
    }

    @Test
    public void trackThatEndsWithZeroAltitudesTest() {
        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "lastPointsHaveZeroAltitude.txt");

        Track cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        Point last = cleaned.points().last();

        assertTrue(last.altitudeIsMissing());
    }

    @Test
    public void trackWithZeroAltitudesInMiddleTest() {
        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "middleOfTrackHasZeroAltitudes.txt");

        Track cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        Point missing = cleaned.points().stream().filter(Point::altitudeIsMissing).findFirst().get();

        assertTrue(!missing.equals(cleaned.points().first()) && !missing.equals(cleaned.points().last()));
    }

    @Test
    public void smoothTrackWithAllZeroAltitudesTest() {

        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "allPointsHaveZeroAltitude.txt");
        Optional<Track> cleaned = coreSmoothing().clean(track);

        assertFalse(cleaned.isPresent());
    }

    @Test
    public void smoothTrackThatStartsWithZeroAltitudesTest() {

        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "firstPointsHaveZeroAltitude.txt");
        Track cleaned = coreSmoothing().clean(track).get();

        assertEquals(40000.0, cleaned.points().first().altitude().inFeet(), 0.1);
    }

    @Test
    public void smoothTrackThatEndsWithZeroAltitudesTest() {

        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "lastPointsHaveZeroAltitude.txt");
        Track cleaned = coreSmoothing().clean(track).get();

        assertEquals(19600.0, cleaned.points().last().altitude().inFeet(), 0.1);
    }

    @Test
    public void smoothTrackWithZeroAltitudesInMiddleTest() {

        Track track = createTrackFromResource(ZeroAltitudeToNull.class, "middleOfTrackHasZeroAltitudes.txt");
        Track cleaned = coreSmoothing().clean(track).get();

        Optional<? extends Point> missing = cleaned.points().stream().filter(pt -> pt.altitudeIsMissing() || pt.altitude().inFeet() <= 0.0).findFirst();

        assertFalse(missing.isPresent());
    }
}


