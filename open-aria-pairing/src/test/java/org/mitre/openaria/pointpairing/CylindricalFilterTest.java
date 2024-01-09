
package org.mitre.openaria.pointpairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.Spherical.feetPerNM;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointBuilder;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.Spherical;

public class CylindricalFilterTest {

    @Test
    public void testHorizontalSeparation() {

        Point p1 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1000.0)).build();
        Point p2 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1000.0)).build();
        Point p3 = (new PointBuilder()).latLong(0.0, 1.0).altitude(Distance.ofFeet(1000.0)).build();

        double MAX_HORIZ_SEPARATION_IN_FT = 1000;
        double MAX_VERT_SEPARATION = 500;

        CylindricalFilter filter = new CylindricalFilter(MAX_HORIZ_SEPARATION_IN_FT, MAX_VERT_SEPARATION);

        assertTrue(filter.test(Pair.of(p1, p1)), "A point is in the same cylindar with itself");
        assertTrue(filter.test(Pair.of(p1, p2)), "A point is in the same cylindar with itself");

        //manually compute distance
        double distanceInNM = Spherical.distanceInNM(
            0.0, 0.0,
            0.0, 1.0
        );

        assertTrue(distanceInNM * feetPerNM() > MAX_HORIZ_SEPARATION_IN_FT, "confirm distance is big");
        assertFalse(filter.test(Pair.of(p1, p3)), "confirm filter rejects points that are far apart");
    }

    @Test
    public void testVertSeparation() {

        Point p1 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1000.0)).build();
        Point p2 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1000.0)).build();
        Point p3 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1500.0)).build();
        Point p4 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1501.0)).build();
        Point p5 = (new PointBuilder()).latLong(0.0, 0.0).build();

        double MAX_HORIZ_SEPARATION_IN_FT = 1000;
        double MAX_VERT_SEPARATION = 500;

        CylindricalFilter filter = new CylindricalFilter(MAX_HORIZ_SEPARATION_IN_FT, MAX_VERT_SEPARATION);

        assertTrue(filter.test(Pair.of(p1, p1)), "A point is in the same cylindar with itself");
        assertTrue(filter.test(Pair.of(p1, p2)), "A point is in the same cylindar with itself");
        assertTrue(filter.test(Pair.of(p1, p3)), "These points are 500ft apart");
        assertFalse(filter.test(Pair.of(p1, p4)), "These points are 500ft apart");
        assertFalse(filter.test(Pair.of(p1, p5)), "Missing altitude data should be rejected");
    }

    @Test
    public void testVertSeparation_allowMissingData() {

        Point p1 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1000.0)).build();
        Point p2 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1000.0)).build();
        Point p3 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1500.0)).build();
        Point p4 = (new PointBuilder()).latLong(0.0, 0.0).build();

        double MAX_HORIZ_SEPARATION_IN_FT = 1000;
        double MAX_VERT_SEPARATION = 500;

        CylindricalFilter filter = new CylindricalFilter(MAX_HORIZ_SEPARATION_IN_FT, MAX_VERT_SEPARATION, true);

        assertTrue(filter.test(Pair.of(p1, p1)), "A point is in the same cylindar with itself");
        assertTrue(filter.test(Pair.of(p1, p2)), "A point is in the same cylindar with itself");
        assertTrue(filter.test(Pair.of(p1, p3)), "These points are 500ft apart");
        assertTrue(filter.test(Pair.of(p1, p4)), "These points have no altitude data");
    }

    @Test
    public void testHorizAndVertSeparation() {

        Point p1 = (new PointBuilder()).latLong(0.0, 0.0).altitude(Distance.ofFeet(1000.0)).build();
        Point p2 = (new PointBuilder()).latLong(0.0, 1.0).altitude(Distance.ofFeet(2000.0)).build();

        double MAX_HORIZ_SEPARATION_IN_FT = 1000;
        double MAX_VERT_SEPARATION = 500;

        CylindricalFilter filter = new CylindricalFilter(MAX_HORIZ_SEPARATION_IN_FT, MAX_VERT_SEPARATION, true);

        assertFalse(filter.test(Pair.of(p1, p2)), "A rejection holds when both dimensions fail the test");
    }
}
