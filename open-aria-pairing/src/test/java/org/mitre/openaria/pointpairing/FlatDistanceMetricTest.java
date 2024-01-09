
package org.mitre.openaria.pointpairing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;
import org.mitre.caasd.commons.LatLong;

public class FlatDistanceMetricTest {

    @Test
    public void testConstructor() {
        FlatDistanceMetric metric = new FlatDistanceMetric(1.0, 5.0);
        assertEquals(1.0, metric.timeCoef(), 0.0001);
        assertEquals(5.0, metric.distanceCoef(), 0.0001);
    }

    @Test
    public void testInputPointsWithSameTime() {
        FlatDistanceMetric metric = new FlatDistanceMetric(1.0, 5.0);

        Point p1 = (new PointBuilder()).latLong(LatLong.of(0.0, 0.0)).time(Instant.EPOCH).build();
        Point p2 = (new PointBuilder()).latLong(LatLong.of(0.0, 0.0)).time(Instant.EPOCH).build();

        assertEquals(
            0.0,
            metric.distanceBtw(p1, p2),
            0.0001
        );
    }
}
