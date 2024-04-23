
package org.mitre.openaria.smoothing;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.core.Tracks.createTrackFromFile;

import java.time.Duration;
import java.time.Instant;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class AccelerationOutlierDetectorTest {

    /**
     * The first point of this test track is an acceleration outlier caused by an errant radar hit.
     * Verify that the cleaner removes this first point.
     */
    @Test
    public void testRealDataWithOutlierAtStart() {

        Track rawTrack = createTrackFromFile(getResourceFile("trackWithAccelerationOutlier.txt"));

        DataCleaner<Track> cleaner = MutableSmoother.of(new AccelerationOutlierDetector());
        Track smoothedTrack = cleaner.clean(rawTrack).get();

        Instant startTimeOfRawTrack = rawTrack.points().first().time();
        Instant startTimeOfSmoothTrack = smoothedTrack.points().first().time();

        assertThat("The first point of this track is an acceleration outlier and should be removed",
            startTimeOfRawTrack.isBefore(startTimeOfSmoothTrack));
    }

    @Test
    public void testRealDataWithOutlierInMiddle() {

        Track rawTrack = createTrackFromFile(getResourceFile("trackWithDataGapInMiddle.txt"));

        DataCleaner<Track> cleaner = MutableSmoother.of(
            new AccelerationOutlierDetector(),
            new DistanceDownSampler(),
            new TrackFilter(Duration.ofMillis(20_000))
        );

        Track smoothedTrack = cleaner.clean(rawTrack).get();

        assertThat("The smoothed track should not have speeds over 50 knots.", maxSpeedInKnots(smoothedTrack), lessThan(50.0));
    }

    private double maxSpeedInKnots(Track track) {

        return track.points().stream().map(Point::speedInKnots).max(Double::compare).orElse(0.0);
    }

    @Test
    public void testRealDataWithOutlierAtEnd() {

        Track rawTrack = createTrackFromFile(getResourceFile("trackWithAccelerationOutlierAtEnd.txt"));

        DataCleaner<Track> cleaner = MutableSmoother.of(new AccelerationOutlierDetector());
        Track smoothedTrack = cleaner.clean(rawTrack).get();

        assertThat("The smoothed track should have its last five points removed", smoothedTrack.size() == rawTrack.size() - 5);
    }

    @Test
    public void removeOutlierNearStartOfTrack() {

        Track testTrack = trackWithSingleOutlierInMiddle();
        Track smoothedTrack = MutableSmoother.of(new AccelerationOutlierDetector()).clean(testTrack).get();

        assertThat(
            "Acceleration outliers should have been removed from the beginning of this track",
            smoothedTrack.size() < testTrack.size()
        );
        assertThat(
            "The last point of the test track should not be removed",
            smoothedTrack.points().last().time().equals(testTrack.points().last().time())
        );
    }

    @Test
    public void removeOutlierAtEndOfTrack() {

        Track testTrack = trackWithSingleOutlierAtEnd();
        Track smoothedTrack = MutableSmoother.of(new AccelerationOutlierDetector()).clean(testTrack).get();

        assertThat(
            "The last point is an acceleration outlier and should be removed.",
            smoothedTrack.size() == testTrack.size() - 1
        );
    }

    private Track trackWithSingleOutlierInMiddle() {

        Instant time = EPOCH;
        LatLong position = LatLong.of(0.0, 0.0);
        double nmPerSec = 1.0 / 3600.0; //a speed of 1knot

        TreeSet<Point> points = new TreeSet<>();

        for (int i = 0; i < 5; i++) {
            time = time.plusSeconds(1);
            position = position.projectOut(0.0, 2.0 * nmPerSec);
            points.add(Point.builder().time(time).latLong(position).build());
        }

        time = time.plusSeconds(1);
        position = position.projectOut(89.0, 300 * nmPerSec);
        points.add(Point.builder().time(time).latLong(position).build());

        time = time.plusSeconds(1);
        position = position.projectOut(271.0, 300 * nmPerSec);
        points.add(Point.builder().time(time).latLong(position).build());

        for (int i = 0; i < 20; i++) {
            time = time.plusSeconds(1);
            position = position.projectOut(0.0, 2.0 * nmPerSec);
            points.add(Point.builder().time(time).latLong(position).build());
        }

        return Track.of(points);
    }

    private Track trackWithSingleOutlierAtEnd() {

        int numPoints = 35;
        LatLong position = LatLong.of(0.0, 0.0);
        Instant time = EPOCH;
        double nmPerSec = 1.0 / 3600.0; //a speed of 1knot

        TreeSet<Point> points = new TreeSet<>();

        for (int i = 0; i < numPoints; i++) {
            position = position.projectOut(0.0, 2.0 * nmPerSec);
            time = time.plusSeconds(1);
            points.add(Point.builder().time(time).latLong(position).build());
        }

        points.add(
            Point.builder().time(time.plusSeconds(1))
                .latLong(position.projectOut(0.0, 300.0 * nmPerSec))
                .build()
        );

        return Track.of(points);
    }
}