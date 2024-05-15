
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Time.durationBtw;

import java.time.Duration;
import java.time.Instant;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;

public class TimeDownSamplerTest {

    public static Track<String> testTrack() {
        return Track.of(newArrayList(
            newPoint(EPOCH),
            newPoint(EPOCH.plusSeconds(1)),
            newPoint(EPOCH.plusSeconds(2)),
            newPoint(EPOCH.plusSeconds(3)),
            newPoint(EPOCH.plusSeconds(4)),
            newPoint(EPOCH.plusSeconds(5)),
            newPoint(EPOCH.plusSeconds(6))
        ));
    }

    private static Point<String> newPoint(Instant pointTime) {

        return Point.<String>builder()
            .time(pointTime)
            .latLong(0.0, 0.0)
            .build();
    }

    @Test
    public void testDownSampling() {

        Duration maxTimeDelta = Duration.ofSeconds(5);
        TimeDownSampler<String> smoother = new TimeDownSampler<>(maxTimeDelta);

        Track<String> cleanedTrack = smoother.clean(testTrack()).get();

        Point last = null;
        for (Point point : cleanedTrack.points()) {
            if (last != null) {
                Duration timeDelta = durationBtw(last.time(), point.time());
                assertTrue(timeDelta.toMillis() <= maxTimeDelta.toMillis());
            }
        }

        assertEquals(2, cleanedTrack.size());
        assertEquals(EPOCH, cleanedTrack.points().first().time());
        assertEquals(EPOCH.plusSeconds(5), cleanedTrack.points().last().time());
    }
}
