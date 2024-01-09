
package org.mitre.openaria.smoothing;

import static java.lang.Math.toRadians;
import static org.apache.commons.math3.util.FastMath.cos;
import static org.apache.commons.math3.util.FastMath.sin;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;
import static org.mitre.caasd.commons.Spherical.courseInDegrees;
import static org.mitre.caasd.commons.Spherical.feetPerNM;

import java.time.Duration;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Spherical;
import org.mitre.caasd.commons.math.Vector;


public class LowSpeedAdjustmentTest {

    /**
     * Verify that cleaning a track with a clump of stationary points followed by a period of
     * acceleration does not mistakenly create lateral outliers. If the LowSpeedAdjustment filter is
     * removed from this cleaner, then the test fails.
     */
    @Test
    public void doNotCreateOutlierPointsTest() {
        Track track = createTrackFromResource(
            LowSpeedAdjustment.class,
            "clumpedTrack.txt"
        );

        //LOL LowSpeedAdjustmentTest no longer needs a LowSpeedAdjustment to pass!
        DataCleaner<Track> cleaner = MutableSmoother.of(
            new DistanceDownSampler(20.0 / feetPerNM(), Duration.ofSeconds(30)),
            new TrackFilter(Duration.ofMillis(20_000))  //the new TrackFilter DOES NOT need to be wrapped in a LowSpeedAdjustment
        );

        Track smoothedTrack = cleaner.clean(track).get();

        assertThat("This smoothed track should not have outlier points.", !hasOutlierPoints(smoothedTrack));
    }

    private Boolean hasOutlierPoints(Track track) {
        NavigableSet<? extends Point> points = track.points();
        Point startPoint = points.iterator().next();
        Point endPoint = points.descendingSet().iterator().next();

        Vector vectorFromStartToEnd = vectorBetweenPoints(startPoint, endPoint);

        for (Point point : points) {
            Vector vectorFromStartToPoint = vectorBetweenPoints(startPoint, point);
            if (squareDistanceFromLine(vectorFromStartToEnd, vectorFromStartToPoint) > .00004) {
                return true;
            }
        }

        return false;
    }

    private Vector vectorBetweenPoints(Point point1, Point point2) {
        LatLong latLong1 = point1.latLong();
        LatLong latLong2 = point2.latLong();
        Double courseBetweenPoints = courseInDegrees(latLong1, latLong2);
        double distanceBetweenPoints = Spherical.distanceInNM(latLong1, latLong2);

        return Vector.of(
            distanceBetweenPoints * cos(toRadians(courseBetweenPoints)),
            distanceBetweenPoints * sin(toRadians(courseBetweenPoints))
        );
    }

    private double squareDistanceFromLine(Vector lineVector, Vector pointVector) {
        return pointVector.dot(pointVector) - (pointVector.dot(lineVector) * pointVector.dot(lineVector) / lineVector.dot(lineVector));
    }
}
