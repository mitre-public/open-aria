
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.abs;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Spherical.feetPerNM;

import java.util.List;
import java.util.Random;

import org.mitre.caasd.commons.LatLong;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

import org.junit.jupiter.api.Test;


public class HasLowVariabilityTest {

    static Random RNG = new Random(17L);

    static final Point CENTER_POINT = Point.builder()
        .latLong(50.0, 50.0)
        .time(EPOCH)
        .build();

    @Test
    public void testTrackSizeIsConsidered() {

        HasLowVariability filter = new HasLowVariability(20, 0.1, 1e5);

        MutableTrack shortTestTrack = createTestTrack(19, 0.0);
        MutableTrack longTestTrack = createTestTrack(21, 0.0);

        assertFalse(
            filter.test(shortTestTrack),
            "This Track should not be considered a low variability track because it is too short"
        );
        assertTrue(
            filter.test(longTestTrack),
            "This Track should be considedered a low variability track because it is long enough to match"
        );
    }

    @Test
    public void testReducingTrackLocationVarianceTriggersFilter() {

        HasLowVariability filter = new HasLowVariability(300, 0.2, 1e5);

        double higherDistFromCenterStdDev = 20.0 / feetPerNM();
        MutableTrack highVarienceTrack = createTestTrack(1000, higherDistFromCenterStdDev);

        double lowerDistFromCenterStdDev = 10 / feetPerNM();
        MutableTrack lowVarienceTrack = createTestTrack(1000, lowerDistFromCenterStdDev);

        assertFalse(
            filter.test(highVarienceTrack),
            "A Track with high location variance should not be filtered"
        );

        assertTrue(
            filter.test(lowVarienceTrack),
            "A Track with low location variance should get filtered"
        );
    }

    /* This track contains Points in a guassian distribution centered around CENTER_POINT. */
    private static MutableTrack createTestTrack(int numPoints, double distStandardDev) {
        List<Point> points = newArrayList();

        for (int i = 0; i < numPoints; i++) {
            points.add(gaussianPoint(i, distStandardDev));
        }

        return Track.of((List) points).mutableCopy();
    }

    private static Point gaussianPoint(int i, double distStandardDev) {

        LatLong randomLocation = CENTER_POINT.latLong().projectOut(
            360.0 * RNG.nextDouble(), //0-360 degress
            distStandardDev * abs(RNG.nextGaussian())
        );

        return Point.builder()
            .latLong(randomLocation)
            .time(EPOCH.plusSeconds(i))
            .build();
    }
}
