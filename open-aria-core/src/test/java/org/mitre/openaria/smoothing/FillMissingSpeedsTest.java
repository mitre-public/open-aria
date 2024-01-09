
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.LatLong;


public class FillMissingSpeedsTest {

    public MutableTrack trackWithNullAtTheFront() {

        LatLong position = LatLong.of(0.0, 0.0);
        double nmPerSec = 100.0 / 3600.0; //a speed of 100knots

        MutablePoint p1 = Point.builder()
            .time(EPOCH)
            .latLong(position)
            .buildMutable();

        MutablePoint p2 = Point.builder()
            .time(EPOCH.plusSeconds(5))
            .latLong(position.projectOut(0.0, 5.0 * nmPerSec))
            .speed(100.0)
            .buildMutable();

        MutablePoint p3 = Point.builder()
            .time(EPOCH.plusSeconds(10))
            .latLong(position.projectOut(0.0, 10.0 * nmPerSec))
            .speed(100.0)
            .buildMutable();

        TreeSet set = new TreeSet();
        set.addAll(newArrayList(p1, p2, p3));
        return MutableTrack.of(set);
    }

    public MutableTrack trackWithNullAtTheBack() {

        LatLong position = LatLong.of(0.0, 0.0);
        double nmPerSec = 100.0 / 3600.0; //a speed of 100knots

        MutablePoint p1 = Point.builder()
            .time(EPOCH)
            .speed(100.0)
            .latLong(position)
            .buildMutable();

        MutablePoint p2 = Point.builder()
            .time(EPOCH.plusSeconds(5))
            .latLong(position.projectOut(0.0, 5.0 * nmPerSec))
            .speed(100.0)
            .buildMutable();

        MutablePoint p3 = Point.builder()
            .time(EPOCH.plusSeconds(10))
            .latLong(position.projectOut(0.0, 10.0 * nmPerSec))
            .buildMutable();

        TreeSet set = new TreeSet();
        set.addAll(newArrayList(p1, p2, p3));
        return MutableTrack.of(set);
    }

    public MutableTrack trackWithNullInTheMiddle() {
        LatLong position = LatLong.of(0.0, 0.0);
        double nmPerSec = 100.0 / 3600.0; //a speed of 100knots

        MutablePoint p1 = Point.builder()
            .time(EPOCH)
            .speed(100.0)
            .latLong(position)
            .buildMutable();

        MutablePoint p2 = Point.builder()
            .time(EPOCH.plusSeconds(5))
            .latLong(position.projectOut(0.0, 5.0 * nmPerSec))
            .buildMutable();

        MutablePoint p3 = Point.builder()
            .time(EPOCH.plusSeconds(10))
            .latLong(position.projectOut(0.0, 10.0 * nmPerSec))
            .speed(100.0)
            .buildMutable();

        TreeSet set = new TreeSet();
        set.addAll(newArrayList(p1, p2, p3));
        return MutableTrack.of(set);
    }

    public MutableTrack trackWithMultipleGaps() {
        LatLong position = LatLong.of(0.0, 0.0);
        double nmPerSec = 100.0 / 3600.0; //a speed of 100knots

        MutablePoint p1 = Point.builder()
            .time(EPOCH)
            .speed(100.0)
            .latLong(position)
            .buildMutable();

        MutablePoint p2 = Point.builder()
            .time(EPOCH.plusSeconds(5))
            .latLong(position.projectOut(0.0, 5.0 * nmPerSec))
            .speed(100.0)
            .buildMutable();

        //no speed value
        MutablePoint p3 = Point.builder()
            .time(EPOCH.plusSeconds(10))
            .latLong(position.projectOut(0.0, 10.0 * nmPerSec))
            .buildMutable();

        //no speed value
        MutablePoint p4 = Point.builder()
            .time(EPOCH.plusSeconds(15))
            .latLong(position.projectOut(0.0, 15.0 * nmPerSec))
            .buildMutable();

        MutablePoint p5 = Point.builder()
            .time(EPOCH.plusSeconds(20))
            .latLong(position.projectOut(0.0, 20.0 * nmPerSec))
            .speed(100.0)
            .buildMutable();

        TreeSet set = new TreeSet();
        set.addAll(newArrayList(p1, p2, p3, p4, p5));
        return MutableTrack.of(set);
    }

    public MutableTrack acceleratingTrackWithNoSpeedData() {
        LatLong position = LatLong.of(0.0, 0.0);
        double nmPerSec = 100.0 / 3600.0; //a speed of 100knots

        MutablePoint p1 = Point.builder()
            .time(EPOCH)
            .latLong(position)
            .buildMutable();

        //100 knots from 1 to 2
        MutablePoint p2 = Point.builder()
            .time(EPOCH.plusSeconds(5))
            .latLong(p1.latLong().projectOut(0.0, 5.0 * nmPerSec * 1.0))
            .buildMutable();

        //200 knots from 2 to 3
        MutablePoint p3 = Point.builder()
            .time(EPOCH.plusSeconds(10))
            .latLong(p2.latLong().projectOut(0.0, 5.0 * nmPerSec * 2.0))
            .buildMutable();

        //300 knots from 3 to 4
        MutablePoint p4 = Point.builder()
            .time(EPOCH.plusSeconds(15))
            .latLong(p3.latLong().projectOut(0.0, 5.0 * nmPerSec * 3.0))
            .buildMutable();

        //400 knots from 4 to 5
        MutablePoint p5 = Point.builder()
            .time(EPOCH.plusSeconds(20))
            .latLong(p4.latLong().projectOut(0.0, 5.0 * nmPerSec * 4.0))
            .buildMutable();

        TreeSet set = new TreeSet();
        set.addAll(newArrayList(p1, p2, p3, p4, p5));
        return MutableTrack.of(set);
    }

    private MutableTrack trackWithSinglePoint() {

        MutablePoint p1 = Point.builder()
            .time(EPOCH)
            .latLong(LatLong.of(0.0, 0.0))
            .buildMutable();

        return MutableTrack.of(newArrayList(p1));
    }

    @Test
    public void correctNullSpeedAtFrontOfTrack() {

        MutableTrack testTrack = trackWithNullAtTheFront();
        assertTrue(
            testTrack.points().first().speedInKnots() == null,
            "We start with a null speed in the first point"
        );

        MutableTrack fixedTrack = (new FillMissingSpeeds()).clean(testTrack).get();

        assertTrue(
            fixedTrack.points().first().speedInKnots() != null,
            "We end with a non-null speed in the first point"
        );
    }

    @Test
    public void correctNullSpeedAtEndOfTrack() {

        MutableTrack testTrack = trackWithNullAtTheBack();
        assertTrue(
            testTrack.points().last().speedInKnots() == null,
            "We start with a null speed in the last point"
        );

        MutableTrack fixedTrack = (new FillMissingSpeeds()).clean(testTrack).get();

        assertTrue(
            fixedTrack.points().last().speedInKnots() != null,
            "We end with a non-null speed in the last point"
        );
    }

    @Test
    public void correctNullSpeedInTheMiddleOfTrack() {

        MutableTrack testTrack = trackWithMultipleGaps();
        Point thirdPoint = newArrayList(testTrack.points()).get(2);
        Point fourthPoint = newArrayList(testTrack.points()).get(3);
        assertTrue(thirdPoint.speedInKnots() == null);
        assertTrue(fourthPoint.speedInKnots() == null);

        MutableTrack fixedTrack = (new FillMissingSpeeds()).clean(testTrack).get();
        thirdPoint = newArrayList(fixedTrack.points()).get(2);
        fourthPoint = newArrayList(fixedTrack.points()).get(3);

        // We end with a non-null speed in the middle point
        assertTrue(thirdPoint.speedInKnots() != null);
        assertTrue(fourthPoint.speedInKnots() != null);
    }

    @Test
    public void correctMultipleSpeeds() {

        MutableTrack testTrack = trackWithNullInTheMiddle();
        Point middlePoint = newArrayList(testTrack.points()).get(1);
        assertTrue(
            middlePoint.speedInKnots() == null,
            "We start with a null speed in the middle point"
        );

        MutableTrack fixedTrack = (new FillMissingSpeeds()).clean(testTrack).get();
        Point fixedMiddle = newArrayList(fixedTrack.points()).get(1);

        assertTrue(
            fixedMiddle.speedInKnots() != null,
            "We end with a non-null speed in the middle point"
        );
    }

    @Test
    public void correctAcceleratingSpeeds() {
        MutableTrack testTrack = acceleratingTrackWithNoSpeedData();
        for (MutablePoint point : testTrack.points()) {
            assertNull(point.speedInKnots(), "No speed data to start");
        }
        MutableTrack fixedTrack = (new FillMissingSpeeds()).clean(testTrack).get();
        for (MutablePoint point : fixedTrack.points()) {
            assertNotNull(point.speedInKnots(), "The fixed track has speed data");
        }

        List<MutablePoint> points = newArrayList(fixedTrack.points());
        for (int i = 1; i < points.size(); i++) {
            assertTrue(
                points.get(i - 1).speedInKnots() < points.get(i).speedInKnots(),
                "These speeds are strictly increasing in this track"
            );
        }
    }

    @Test
    public void returnEmptyResultForShortTrack() {

        // this also checks that an exception isn't thrown
        Optional<MutableTrack> actual = new FillMissingSpeeds().clean(trackWithSinglePoint());
        assertFalse(actual.isPresent());
    }
}
