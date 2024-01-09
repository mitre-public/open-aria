
package org.mitre.openaria.smoothing;

import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;

public class LowSpeedGateTest {

    @Test
    public void testLowSpeedPointsAreSetToZero() {

        DataCleaner<Track> smoother = MutableSmoother.of(new LowSpeedGate());

        TreeSet<Point> points = new TreeSet<>();
        for (int i = 0; i < 10; i++) {
            Point point = Point.builder().time(Instant.EPOCH.plusSeconds(i)).speed((double) i).build();
            points.add(point);
        }

        Track track = new SimpleTrack(points);
        Optional<Track> smoothedTrack = smoother.clean(track);

        assertThat("No points should be removed from the track", smoothedTrack.get().size() == 10);
        assertThat("Five points have speeds less than 5 knots, and their speeds should be set to 0",
            zeroSpeedCount(smoothedTrack.get()) == 5);
    }

    private long zeroSpeedCount(Track track) {

        return track.points().stream().filter(pt -> pt.speedInKnots() == 0.0).count();
    }


}