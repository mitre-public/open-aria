
package org.mitre.openaria.smoothing;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Optional;
import java.util.TreeSet;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class SpeedExtrapolatorTest {

    private static MutableSmoother cleaner = MutableSmoother.of(new SpeedExtrapolator());

    @Test
    public void fillInitialSpeeds() {

        TreeSet<Point> points = new TreeSet<>();
        points.add(Point.builder().time(EPOCH).build());
        points.add(Point.builder().time(EPOCH.plusSeconds(1)).build());
        points.add(Point.builder().time(EPOCH.plusSeconds(2)).speed(10.0).build());

        Track track = Track.of( (TreeSet) points);
        Track cleanedTrack = cleaner.clean(track).get();

        assertThat("Missing first speed should be extrapolated from the third point's speed",
            cleanedTrack.nearestPoint(EPOCH).speedInKnots() == 10.0);

        assertThat("Missing second speed should be extrapolated from the third point's speed",
            cleanedTrack.nearestPoint(EPOCH.plusSeconds(1)).speedInKnots() == 10.0);
    }

    @Test
    public void returnEmptyIfNoSpeeds() {

        TreeSet<Point> points = new TreeSet<>();
        points.add(Point.builder().time(EPOCH).build());
        points.add(Point.builder().time(EPOCH.plusSeconds(1)).build());

        Track track = Track.of( (TreeSet) points);
        Optional<Track> cleanedTrack = cleaner.clean(track);

        assertThat("Cleaner should remove a track without any speeds", !cleanedTrack.isPresent());
    }

    @Test
    public void doNotFillNonInitialSpeeds() {

        TreeSet<Point> points = new TreeSet<>();
        points.add(Point.builder().time(EPOCH).speed(1.0).build());
        points.add(Point.builder().time(EPOCH.plusSeconds(1)).build());

        Track track = Track.of( (TreeSet) points);
        Track cleanedTrack = cleaner.clean(track).get();

        assertThat("Cleaner should not fill missing speeds if they occur after the first nonnull speed",
            cleanedTrack.nearestPoint(EPOCH.plusSeconds(1)).speedInKnots() == null);
    }
}