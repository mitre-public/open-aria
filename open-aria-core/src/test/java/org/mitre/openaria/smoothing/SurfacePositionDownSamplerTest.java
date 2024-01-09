
package org.mitre.openaria.smoothing;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;
import static org.mitre.caasd.commons.Spherical.feetPerNM;

import java.time.Duration;
import java.time.Instant;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;


public class SurfacePositionDownSamplerTest {

    @Test
    public void testStationaryTrack() {

        Track track = stationaryTrack();

        DataCleaner<Track> cleaner = MutableSmoother.of(new SurfacePositionDownSampler(15.0 / feetPerNM(), Duration.ofSeconds(5)));
        Track smoothedTrack = cleaner.clean(track).get();

        assertThat("DownSampler should remove more than half the points.", smoothedTrack.size() < track.size() / 2);
    }

    @Test
    public void testSlowMovingTrack() {

        Track track = slowMovingTrack();

        DataCleaner<Track> cleaner = MutableSmoother.of(new SurfacePositionDownSampler(15.0 / feetPerNM(), Duration.ofSeconds(5)));
        Track smoothedTrack = cleaner.clean(track).get();

        assertThat("DownSampler should remove more than half the points.", smoothedTrack.size() < track.size() / 2);
    }

    /**
     * This test will fail if {@link SurfacePositionDownSampler} is replaced by {@link
     * DistanceDownSampler}.
     */
    @Test
    public void testStopAndStartSurfaceTrack() {

        Track rawTrack = createTrackFromResource(
            SurfacePositionDownSampler.class,
            "stoppingTrack.txt"
        );

        DataCleaner<Track> surfaceSmoother = MutableSmoother.of(
            new RemoveLowVariabilityTracks(new HasLowVariability(1, 0.1, 2e4)),
            new DuplicateTimeRemover(),
            new AccelerationOutlierDetector(),
            new FillMissingSpeeds(),
            new FillMissingAltitudes(),
            new SurfacePositionDownSampler(),
            new LateralOutlierDetector(),
            new TrackFilter(Duration.ofMillis(20_000))
        );

        Track smoothedTrack = surfaceSmoother.clean(rawTrack).get();

        Instant stoppingTime = firstTimeOfContinuousLowSpeed(rawTrack, 2.0);
        Double smoothedSpeedAtStoppingTime = smoothedTrack.interpolatedPoint(stoppingTime).get().speedInKnots();

        assertThat("When the track has stopped, the smoothed speed should not exceed 1 knot", smoothedSpeedAtStoppingTime, lessThan(1.0));
    }

    private Instant firstTimeOfContinuousLowSpeed(Track track, double speedThreshold) {

        Instant firstTimeBelowSpeedThreshold = null;
        int numLowSpeedPointsThreshold = 5;
        int sequentialLowSpeedPoints = 0;

        for (Point point : track.points()) {
            if (point.speedInKnots() <= speedThreshold) {
                sequentialLowSpeedPoints++;
                if (sequentialLowSpeedPoints == numLowSpeedPointsThreshold) {
                    firstTimeBelowSpeedThreshold = point.time();
                    break;
                }
            } else {
                sequentialLowSpeedPoints = 0;
            }
        }

        return firstTimeBelowSpeedThreshold;
    }

    private Track stationaryTrack() {

        LatLong position = LatLong.of(0.0, 0.0);
        Instant time = EPOCH;
        double speedInKnots = 0.0;
        double course = 0.0;

        TreeSet<Point> points = new TreeSet<>();

        for (int i = 0; i < 30; i++) {
            time = time.plusSeconds(1);
            points.add(Point.builder()
                .time(time)
                .latLong(position)
                .speed(speedInKnots)
                .courseInDegrees(course)
                .build()
            );
        }

        return new SimpleTrack(points);
    }

    private Track slowMovingTrack() {
        LatLong position = LatLong.of(0.0, 0.0);
        Instant time = EPOCH;
        double nmPerSec = 4.0 / 3600.0; //a speed of 4knots

        TreeSet<Point> points = new TreeSet<>();

        for (int i = 0; i < 20; i++) {
            position = position.projectOut(20.0, nmPerSec);
            time = time.plusSeconds(1);
            points.add(
                Point.builder()
                    .time(time)
                    .latLong(position)
                    .speed(4.0)
                    .courseInDegrees(20.0)
                    .build()
            );
        }

        return new SimpleTrack(points);
    }
}