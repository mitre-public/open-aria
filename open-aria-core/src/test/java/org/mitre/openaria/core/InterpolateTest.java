
package org.mitre.openaria.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mitre.openaria.core.Interpolate.interpolate;
import static org.mitre.openaria.core.Interpolate.interpolateCourse;
import static org.mitre.openaria.core.Interpolate.interpolateLatLong;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Spherical;

public class InterpolateTest {

    @Test
    public void testInterpolateCourseOnBadInput1() {

        try {
            interpolateCourse(5.0, 10.0, 1.2);
            fail("Should not work because fraction 1.2 is out of range");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("The fraction: "));
        }
    }

    @Test
    public void testInterpolateCourseOnBadInput2() {

        try {
            interpolateCourse(5.0, 10.0, -0.1);
            fail("Should not work because fraction -0.1 is out of range");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("The fraction: "));
        }
    }

    @Test
    public void testInterpolateCourseOnNullInput() {

        assertNull(interpolateCourse(null, 10.0, .5));
        assertNull(interpolateCourse(10.0, null, .5));
    }

    @Test
    public void testInterpolateCourse_bug() {
        //A bug was found when interpolating between a Point with course 178 and a Point with 181
        double TOL = 0.001;
        assertEquals(
            178.0,
            interpolateCourse(178.0, 181.0, 0.0),
            TOL
        );
        assertEquals(
            181.0,
            interpolateCourse(178.0, 181.0, 1.0),
            TOL
        );
        //this assertion failed ... result was 106.6
        assertEquals(
            178.6,
            interpolateCourse(178.0, 181.0, 0.2),
            TOL
        );
    }

    @Test
    public void testInterpolateCourse() {
        /*
         * Confirm interpolateCourse work (espiecially when course goes from values like 354 to 4)
         */
        double TOL = 0.001;

        assertEquals(
            5.0,
            interpolateCourse(5.0, 10.0, 0.0),
            TOL
        );

        assertEquals(
            10.0,
            interpolateCourse(5.0, 10.0, 1.0),
            TOL
        );

        assertEquals(
            5.0,
            interpolateCourse(5.0, 355.0, 0.0),
            TOL
        );

        assertEquals(
            355.0,
            interpolateCourse(5.0, 355.0, 1.0),
            TOL
        );

        assertEquals(
            0.0,
            interpolateCourse(5.0, 355.0, 0.5),
            TOL
        );

        assertEquals(
            350.0,
            interpolateCourse(330.0, 20.0, 0.4),
            TOL
        );

        assertEquals(
            350.0,
            interpolateCourse(20.0, 330.0, 0.6),
            TOL
        );
    }

    @Test
    public void testInterpolateLatLong() {

        LatLong p1 = new LatLong(0.0, 5.0);
        LatLong p2 = new LatLong(5.0, 0.0);

        double TOLERANCE = 0.0001;

        assertEquals(
            0.0,
            interpolateLatLong(p1, p2, 0.0).latitude(),
            TOLERANCE
        );
        assertEquals(
            5.0,
            interpolateLatLong(p1, p2, 1.0).latitude(),
            TOLERANCE
        );

        assertEquals(
            5.0,
            interpolateLatLong(p1, p2, 0.0).longitude(),
            TOLERANCE
        );
        assertEquals(
            0.0,
            interpolateLatLong(p1, p2, 1.0).longitude(),
            TOLERANCE
        );
    }

    @Test
    public void testUnsafeInterpolateLatLongFails() {

        LatLong p1 = new LatLong(89.0, 0.0);
        LatLong p2 = new LatLong(-89.0, 0.0);

        try {
            interpolateLatLong(p1, p2, 0.5);
            fail("This call should fail until the implementation is improved");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("Interpolation is unsafe at this distance (latitude)"));
        }

        LatLong p3 = new LatLong(0.0, -178.0);
        LatLong p4 = new LatLong(0.0, 178.0);

        try {
            interpolateLatLong(p3, p4, 0.5);
            fail("This call should fail until the implementation is improved");
        } catch (IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("Interpolation is unsafe at this distance (longitude)"));
        }
    }

    @Test
    public void testInterpolatePoint() {

        Point p1 = (new PointBuilder())
            .time(Instant.EPOCH)
            .altitude(Distance.ofFeet(1000.0))
            .courseInDegrees(120.0)
            .latLong(new LatLong(0.0, 10.0))
            .speed(200.0)
            .build();

        Point p2 = (new PointBuilder())
            .time(Instant.EPOCH.plusSeconds(8))
            .altitude(Distance.ofFeet(500.0))
            .courseInDegrees(130.0)
            .latLong(new LatLong(5.0, 15.0))
            .speed(300.0)
            .build();

        Point testPoint = interpolate(p1, p2, Instant.EPOCH.plusSeconds(4));

        double TOLERANCE = 0.0001;

        assertEquals(
            Instant.EPOCH.plusSeconds(4),
            testPoint.time()
        );

        assertEquals(
            750.0,
            testPoint.altitude().inFeet(),
            TOLERANCE
        );
        assertEquals(
            125.0,
            testPoint.course(),
            TOLERANCE
        );

        assertEquals(LatLong.of(2.5, 12.5), testPoint.latLong());

        assertEquals(
            250.0,
            testPoint.speedInKnots(),
            TOLERANCE
        );
    }

    @Test
    public void testInterpolatePoint2() {
        /*
         * Test the interpolation works properly at the "start" of the timewindow
         */

        Point p1 = (new PointBuilder())
            .time(Instant.EPOCH)
            .altitude(Distance.ofFeet(1000.0))
            .courseInDegrees(120.0)
            .latLong(new LatLong(0.0, 10.0))
            .speed(200.0)
            .build();

        Point p2 = (new PointBuilder())
            .time(Instant.EPOCH.plusSeconds(8))
            .altitude(Distance.ofFeet(500.0))
            .courseInDegrees(130.0)
            .latLong(new LatLong(5.0, 15.0))
            .speed(300.0)
            .build();

        Point testPoint = interpolate(p1, p2, Instant.EPOCH);

        double TOLERANCE = 0.0001;

        assertEquals(
            Instant.EPOCH,
            testPoint.time()
        );

        assertEquals(
            1000.0,
            testPoint.altitude().inFeet(),
            TOLERANCE
        );
        assertEquals(
            120.0,
            testPoint.course(),
            TOLERANCE
        );

        assertEquals(LatLong.of(0.0, 10.0), testPoint.latLong());

        assertEquals(
            200.0,
            testPoint.speedInKnots(),
            TOLERANCE
        );
    }

    @Test
    public void testInterpolatePoint3() {
        /*
         * Test the interpolation works properly at the "end" of the timewindow
         */

        Point p1 = (new PointBuilder())
            .time(Instant.EPOCH)
            .altitude(Distance.ofFeet(1000.0))
            .courseInDegrees(120.0)
            .latLong(new LatLong(0.0, 10.0))
            .speed(200.0)
            .build();

        Point p2 = (new PointBuilder())
            .time(Instant.EPOCH.plusSeconds(8))
            .altitude(Distance.ofFeet(500.0))
            .courseInDegrees(130.0)
            .latLong(new LatLong(5.0, 15.0))
            .speed(300.0)
            .build();

        Point testPoint = interpolate(p1, p2, Instant.EPOCH.plusSeconds(8));

        double TOLERANCE = 0.0001;

        assertEquals(
            Instant.EPOCH.plusSeconds(8),
            testPoint.time()
        );

        assertEquals(
            500.0,
            testPoint.altitude().inFeet(),
            TOLERANCE
        );
        assertEquals(
            130.0,
            testPoint.course(),
            TOLERANCE
        );

        assertEquals(LatLong.of(5.0, 15.0), testPoint.latLong());

        assertEquals(
            300.0,
            testPoint.speedInKnots(),
            TOLERANCE
        );
    }

    @Test
    public void testInterpolatePointWithBadNulCourse() {

        //The RH message shown in s1 has no course
        String s1 = "[RH],STARS,GEG,07/08/2017,14:09:11.474,,,,1200,0,0,,47.61734,-117.54339,655,0,0.3008,-0.1445,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}\n";
        //The RH message shown in s2 has a course of 253
        String s2 = "[RH],STARS,GEG,07/08/2017,14:09:16.284,,,,1200,28,88,253,47.61675,-117.54618,655,0,0.1876,-0.1797,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}\n";

        Point p1 = NopPoint.from(s1);
        Point p2 = NopPoint.from(s2);

        Point interpolated = interpolate(p1, p2, p1.time().plusSeconds(2));

        assertThat(
            "The course of the interpolated point is set even though the 1st point had no ocourse",
            Spherical.courseInDegrees(p1.latLong(), p2.latLong()),
            is(interpolated.course())
        );
    }

}
