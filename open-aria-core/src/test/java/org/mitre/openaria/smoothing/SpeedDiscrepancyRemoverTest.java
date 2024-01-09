
package org.mitre.openaria.smoothing;

import static java.time.Instant.EPOCH;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.LatLong;

public class SpeedDiscrepancyRemoverTest {

    private static final MutableSmoother CLEANER = MutableSmoother.of(new SpeedDiscrepancyRemover());

    @Test
    public void removeMovingPointsWithZeroRawSpeed() {

        TreeSet<Point> points = new TreeSet<>();
        LatLong position = LatLong.of(40.0, -80.0);

        points.add(Point.builder().time(EPOCH).latLong(position).speed(0.0).build());
        position = position.projectOut(0.0, 16.0 / 3600.0);
        points.add(Point.builder().time(EPOCH.plusSeconds(1)).latLong(position).speed(0.0).build()); // <-- should be dropped
        points.add(Point.builder().time(EPOCH.plusSeconds(2)).latLong(position).speed(0.0).build());
        points.add(Point.builder().time(EPOCH.plusSeconds(3)).latLong(position).speed(0.0).build());

        Track cleanedTrack = cleanTrack(points);

        Optional<Point> badPoint = pointAt(cleanedTrack, EPOCH.plusSeconds(1));
        assertThat("Second point should be removed because the average speed is 8 knots but the raw speed is 0",
            not(badPoint.isPresent()));
    }

    @Test
    public void removeStaticPointsWithPositiveRawSpeed() {

        TreeSet<Point> points = new TreeSet<>();
        LatLong position = LatLong.of(0.0, 0.0);

        points.add(Point.builder().time(EPOCH).latLong(position).speed(0.0).build());
        points.add(Point.builder().time(EPOCH.plusSeconds(1)).latLong(position).speed(0.0).build());
        points.add(Point.builder().time(EPOCH.plusSeconds(2)).latLong(position).speed(7.0).build()); // <-- should be dropped
        points.add(Point.builder().time(EPOCH.plusSeconds(3)).latLong(position).speed(0.0).build());

        Track cleanedTrack = cleanTrack(points);

        Optional<Point> badPoint = pointAt(cleanedTrack, EPOCH.plusSeconds(2));
        assertThat("Third point should be removed because the raw speed is 7 knots but track is not moving",
            not(badPoint.isPresent()));
    }

    @Test
    public void removeMovingPointsWithTooLargeRawSpeed() {

        TreeSet<Point> points = new TreeSet<>();
        LatLong position = LatLong.of(40.0, -80.0);

        points.add(Point.builder().time(EPOCH).latLong(position).speed(100.0).build());
        position = position.projectOut(0.0, 50.0 / 3600.0);
        points.add(Point.builder().time(EPOCH.plusSeconds(1)).latLong(position).speed(100.0).build()); // <-- should be dropped
        position = position.projectOut(0.0, 50.0 / 3600.0);
        points.add(Point.builder().time(EPOCH.plusSeconds(2)).latLong(position).speed(100.0).build());
        position = position.projectOut(0.0, 100.0 / 3600.0);
        points.add(Point.builder().time(EPOCH.plusSeconds(3)).latLong(position).speed(100.0).build());

        Track cleanedTrack = cleanTrack(points);
        Optional<Point> badPoint = pointAt(cleanedTrack, EPOCH.plusSeconds(1));

        assertThat("Second point should be removed because the average speed is 50 knots, and the raw speed is 100 knots",
            not(badPoint.isPresent()));
    }

    @Test
    public void removeMovingPointsWithTooSmallRawSpeed() {

        TreeSet<Point> points = new TreeSet<>();
        LatLong position = LatLong.of(0.0, 0.0);

        points.add(Point.builder().time(EPOCH).latLong(position).speed(50.0).build());
        position = position.projectOut(0.0, 50.0 / 3600.0);
        points.add(Point.builder().time(EPOCH.plusSeconds(1)).latLong(position).speed(50.0).build());
        position = position.projectOut(0.0, 90.0 / 3600.0);
        points.add(Point.builder().time(EPOCH.plusSeconds(2)).latLong(position).speed(50.0).build()); // <-- should be dropped
        position = position.projectOut(0.0, 90.0 / 3600.0);
        points.add(Point.builder().time(EPOCH.plusSeconds(3)).latLong(position).speed(50.0).build());

        Track cleanedTrack = cleanTrack(points);
        Optional<Point> badPoint = pointAt(cleanedTrack, EPOCH.plusSeconds(2));

        assertThat("Third point should be removed because the average speed is 90 knots, and the raw speed is 50 knots",
            not(badPoint.isPresent()));
    }

    private Track cleanTrack(TreeSet<Point> trackPoints) {
        return CLEANER.clean(new SimpleTrack(trackPoints))
            .orElseThrow(() -> new AssertionError("SpeedDiscrepancyRemover should not remove track"));
    }

    private Optional<Point> pointAt(Track track, Instant time) {
        return (Optional<Point>) track.points().stream()
            .filter(p -> p.time().equals(time))
            .findFirst();
    }
}