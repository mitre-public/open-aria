
package org.mitre.openaria.pointpairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.CommonPoint;
import org.mitre.openaria.core.PointBuilder;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Spherical;

public class PointDistanceMetricTest {

    @Test
    public void testPointsRequireTime() {

        PointDistanceMetric metric = new PointDistanceMetric(1.0, 1.0);

        CommonPoint point = new PointBuilder()
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> metric.distanceBtw(point, point),
            "Points should require time"
        );
    }

    @Test
    public void testPointsRequireLatLong() {

        PointDistanceMetric metric = new PointDistanceMetric(1.0, 1.0);

        CommonPoint point = new PointBuilder()
            .altitude(Distance.ofFeet(0.0))
            .time(Instant.EPOCH)
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> metric.distanceBtw(point, point),
            "Points should require latitude and longitude"
        );
    }

    @Test
    public void testPointsRequireAltitude() {

        PointDistanceMetric metric = new PointDistanceMetric(1.0, 1.0);

        CommonPoint point = new PointBuilder()
            .latLong(0.0, 0.0)
            .time(Instant.EPOCH)
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> metric.distanceBtw(point, point),
            "Points should require altitude"
        );
    }

    @Test
    public void testTimeComputation() {

        PointDistanceMetric metric = new PointDistanceMetric(1.0, 1.0);
        PointDistanceMetric metric2 = new PointDistanceMetric(2.0, 1.0);

        Instant time1 = Instant.EPOCH;
        Instant time2 = time1.plusSeconds(1L);

        CommonPoint p1 = new PointBuilder()
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .time(time1)
            .build();

        CommonPoint p2 = new PointBuilder()
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .time(time2)
            .build();

        double TOLERANCE = 0.00001;

        assertEquals(
            1000.0, metric.distanceBtw(p1, p2), TOLERANCE,
            "A 1 second time difference should produce a distance of 1000.0 (when coef = 1.0)"
        );

        assertEquals(
            1000.0, metric.distanceBtw(p2, p1), TOLERANCE,
            "Switching the points shouldn't change the distance measurement"
        );

        assertEquals(
            2000.0, metric2.distanceBtw(p1, p2), TOLERANCE,
            "A 1 second time difference should produce a distance of 2000.0 (when coef = 2.0)"
        );

        assertEquals(
            2000.0, metric2.distanceBtw(p2, p1), TOLERANCE,
            "Switching the points shouldn't change the distance measurement"
        );
    }

    @Test
    public void testDistanceComputation_Altitude() {

        PointDistanceMetric metric = new PointDistanceMetric(1.0, 1.0);
        PointDistanceMetric metric2 = new PointDistanceMetric(1.0, 2.0);

        CommonPoint p1 = new PointBuilder()
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .time(Instant.EPOCH)
            .build();

        CommonPoint p2 = new PointBuilder()
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(1000.0))
            .time(Instant.EPOCH)
            .build();

        double TOLERANCE = 0.00001;

        assertEquals(
            1000.0, metric.distanceBtw(p1, p2), TOLERANCE,
            "A 1000 ft difference in altitude should produce a distance of 1000.0 (when coef = 1.0)"
        );

        assertEquals(
            2000.0, metric2.distanceBtw(p1, p2), TOLERANCE,
            "A 1000 ft difference in altitude should produce a distance of 2000.0 (when coef = 2.0)"
        );
    }

    @Test
    public void testDistanceComputation_latitude() {

        PointDistanceMetric metric1 = new PointDistanceMetric(1.0, 1.0);
        PointDistanceMetric metric2 = new PointDistanceMetric(1.0, 2.0);

        CommonPoint p1 = new PointBuilder()
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .time(Instant.EPOCH)
            .build();

        CommonPoint p2 = new PointBuilder()
            .latLong(1.0, 1.0)
            .altitude(Distance.ofFeet(0.0))
            .time(Instant.EPOCH)
            .build();

        double TOL = 0.00001;

        assertTrue(metric1.distanceBtw(p1, p2) != 0.0);

        assertEquals(
            2.0 * metric1.distanceBtw(p1, p2), metric2.distanceBtw(p1, p2), TOL,
            "Metric2 applies a coefficient of 2 on distance, thus metric1 needs to be expanded to match"
        );

        LatLong pair1 = new LatLong(0.0, 0.0);
        LatLong pair2 = new LatLong(1.0, 1.0);
        double DIST_IN_NM = pair1.distanceInNM(pair2);

        double DIST_IN_FT = Spherical.feetPerNM() * DIST_IN_NM;
        double SOME_TOL = DIST_IN_FT * 0.005;

        assertEquals(
            DIST_IN_FT, metric1.distanceBtw(p1, p2), SOME_TOL,
            "The measure distance should be within 0.5% of the REAL distance"
        );
    }

    @Test
    public void testDistanceComputation_longitude() {

        PointDistanceMetric metric1 = new PointDistanceMetric(1.0, 1.0);
        PointDistanceMetric metric2 = new PointDistanceMetric(1.0, 2.0);

        CommonPoint p1 = new PointBuilder()
            .latLong(0.0, 0.0)
            .altitude(Distance.ofFeet(0.0))
            .time(Instant.EPOCH)
            .build();

        CommonPoint p2 = new PointBuilder()
            .latLong(0.0, 1.0)
            .altitude(Distance.ofFeet(0.0))
            .time(Instant.EPOCH)
            .build();

        double TOLERANCE = 0.00001;

        assertTrue(metric1.distanceBtw(p1, p2) != 0.0);

        assertEquals(
            2.0 * metric1.distanceBtw(p1, p2), metric2.distanceBtw(p1, p2), TOLERANCE,
            "Metric2 applies a coefficient of 2 on distance, thus metric1 needs to be expanded to match"
        );
    }

}
