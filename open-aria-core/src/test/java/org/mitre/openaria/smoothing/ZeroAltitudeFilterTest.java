package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;
import static org.mitre.openaria.smoothing.TrackSmoothing.coreSmoothing;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

public class ZeroAltitudeFilterTest {

    @Test
    public void trackWithAllZeroAltitudesTest() {
        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "allPointsHaveZeroAltitude.txt").mutableCopy();

        MutableTrack cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        Optional<MutablePoint> any = cleaned.points().stream().filter(mp -> !mp.altitudeIsMissing()).findAny();

        assertFalse(any.isPresent());
    }

    @Test
    public void trackThatStartsWithZeroAltitudesTest() {
        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "firstPointsHaveZeroAltitude.txt").mutableCopy();

        MutableTrack cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        MutablePoint first = cleaned.points().first();

        assertTrue(first.altitudeIsMissing());
    }

    @Test
    public void trackThatEndsWithZeroAltitudesTest() {
        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "lastPointsHaveZeroAltitude.txt").mutableCopy();

        MutableTrack cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        MutablePoint last = cleaned.points().last();

        assertTrue(last.altitudeIsMissing());
    }

    @Test
    public void trackWithZeroAltitudesInMiddleTest() {
        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "middleOfTrackHasZeroAltitudes.txt").mutableCopy();

        MutableTrack cleaned = (new ZeroAltitudeToNull()).clean(track).get();
        MutablePoint missing = cleaned.points().stream().filter(Point::altitudeIsMissing).findFirst().get();

        assertTrue(!missing.equals(cleaned.points().first()) && !missing.equals(cleaned.points().last()));
    }

    @Test
    public void smoothTrackWithAllZeroAltitudesTest() {

        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "allPointsHaveZeroAltitude.txt").mutableCopy();
        Optional<Track> cleaned = coreSmoothing().clean(track);

        assertFalse(cleaned.isPresent());
    }

    @Test
    public void smoothTrackThatStartsWithZeroAltitudesTest() {

        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "firstPointsHaveZeroAltitude.txt").mutableCopy();
        Track cleaned = coreSmoothing().clean(track).get();

        assertEquals(40000.0, cleaned.points().first().altitude().inFeet(), 0.1);
    }

    @Test
    public void smoothTrackThatEndsWithZeroAltitudesTest() {

        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "lastPointsHaveZeroAltitude.txt").mutableCopy();
        Track cleaned = coreSmoothing().clean(track).get();

        assertEquals(19600.0, cleaned.points().last().altitude().inFeet(), 0.1);
    }

    @Test
    public void smoothTrackWithZeroAltitudesInMiddleTest() {

        MutableTrack track = createTrackFromResource(ZeroAltitudeToNull.class, "middleOfTrackHasZeroAltitudes.txt").mutableCopy();
        Track cleaned = coreSmoothing().clean(track).get();

        Optional<? extends Point> missing = cleaned.points().stream().filter(pt -> pt.altitudeIsMissing() || pt.altitude().inFeet() <= 0.0).findFirst();

        assertFalse(missing.isPresent());
    }
}


