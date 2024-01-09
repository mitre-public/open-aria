
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.Instant.EPOCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;

import java.time.Duration;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.EphemeralPoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.NopPoint;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Speed;


public class LateralOutlierDetectorTest {

    @Test
    public void testFindingOneOutlier() {

        //this point is the lateral outlier if the test data
        Point theOutlier = NopPoint.from("[RH],STARS,D21_B,03/24/2018,14:57:02.226,N518SP,C172,,5256,031,109,184,042.46462,-083.75121,3472,5256,-16.5222,15.1222,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}");

        MutableTrack testTrack = createTrackFromResource(
            LateralOutlierDetector.class,
            "oneLateralOutlierTest.txt"
        ).mutableCopy();

        NavigableSet<Point> outliers = (new LateralOutlierDetector()).getOutliers(testTrack);

        assertEquals(1, outliers.size());
        assertEquals(outliers.first().time(), theOutlier.time());

        int sizeBeforeCleaning = testTrack.size();
        Track cleanedTrack = (new LateralOutlierDetector()).clean(testTrack).get();

        assertEquals(sizeBeforeCleaning, cleanedTrack.size() + 1);
        assertNotEquals(cleanedTrack.nearestPoint(theOutlier.time()).time(), theOutlier.time());
    }

    @Test
    public void testCurvyTrack() {

        //this test track is nice and smooth -- it shouldn't flag any outliers (even in the curves)
        MutableTrack testTrack = createTrackFromResource(
            LateralOutlierDetector.class,
            "curvyTrack.txt"
        ).mutableCopy();

        NavigableSet<Point> outliers = (new LateralOutlierDetector()).getOutliers(testTrack);

        assertEquals(0, outliers.size());

        Track cleanedTrack = (new LateralOutlierDetector()).clean(testTrack).get();

        assertEquals(testTrack.size(), cleanedTrack.size());
    }

    @Test
    public void testNearPerfectTrackWithMinorJitter() {

        MutableTrack testTrack = trackWithVeryMinorJitter();

        LateralOutlierDetector outlierDetector = new LateralOutlierDetector();

        NavigableSet<Point> outliers = outlierDetector.getOutliers(testTrack);
        assertEquals(0, outliers.size());

        Track cleanedTrack = outlierDetector.clean(testTrack).get();

        assertEquals(testTrack.size(), cleanedTrack.size());
    }

    /**
     * @return A Track with points that travel in a near perfect straight line. One of the points in
     *     this track was shifted by a few feet. This single point breaks the perfect pattern
     *     despite being a very small distance of "perfect". This test case is designed to catch
     *     situations in which a zero error "degrades" to a ridiculously tiny error (i.e. no outlier
     *     should be detected even though the error increased "dramatically" (when compared to zero
     *     error))
     */
    public static MutableTrack trackWithVeryMinorJitter() {

        LinkedList<Point> points = newLinkedList();

        Point cur = Point.builder().latLong(10.0, 10.0).time(EPOCH).build();
        points.add(cur);

        int NUM_POINTS = 30;
        double NORTH_EAST = 45.0;
        double NORTH_WEST = 360 - 45;
        Duration timeStep = Duration.ofSeconds(5);
        Distance dist = Speed.of(200, KNOTS).times(timeStep);

        while (points.size() < NUM_POINTS) {
            Point last = points.getLast();
            Point next = Point.builder()
                .latLong(last.latLong().projectOut(NORTH_EAST, dist.inNauticalMiles()))
                .time(last.time().plus(timeStep))
                .build();
            points.add(next);
        }

        //manipulate the 15th point very slightly
        Point prior = points.get(14);
        LatLong newPosition = prior.latLong().projectOut(NORTH_WEST, 0.1); //adjust location 0.01 NM
        Point adjustedPoint = Point.builder(prior).butLatLong(newPosition).build();
        points.set(14, adjustedPoint);

        return new SimpleTrack(points).mutableCopy();
    }

    @Test
    public void testRealTrackWithGentalError() {

        // NOT GETTING OUTLIER AT: 21:15:25
        //THESE ARE REASONABLE OUTLIERS
        String outlier1 = "[RH],STARS,ABE_B,03/25/2018,21:39:54.596,N317A,SR22,,0224,032,161,267,040.49013,-075.64892,2326,0224,-9.5386,-9.7261,1,D,A,ABE,RDG,PTW,ABE,2114,ABE,ACT,VFR,,00957,,,,,,S,1,,0,{RH}";
        String outlier2 = "[RH],STARS,ABE_B,03/25/2018,21:45:18.207,N317A,SR22,,0224,033,170,269,040.48656,-075.97119,2326,0224,-24.2925,-9.8784,0,4,A,ABE,RDG,PTW,ABE,2114,ABE,ACT,VFR,,00957,,,,,,S,1,,0,{RH}";

        Set<String> knownOutliers = newHashSet(
            EphemeralPoint.from(NopPoint.from(outlier1)).asNop(),
            EphemeralPoint.from(NopPoint.from(outlier2)).asNop()
        );

        MutableTrack testTrack = createTrackFromResource(
            LateralOutlierDetector.class,
            "trackWithSomeGentalError.txt"
        ).mutableCopy();

        LateralOutlierDetector outlierDetector = new LateralOutlierDetector();

        NavigableSet<Point> outliers = outlierDetector.getOutliers(testTrack);

        assertEquals(2, outliers.size(), "We found exactly 2 outliers");
        //and they are the two shown above
        for (Point outlier : outliers) {
            assertTrue(knownOutliers.contains(outlier.asNop()));
        }

        //using the outlierDetector's "clean" method will mutate the input, save the prior state
        int sizeBeforeCleaning = testTrack.size();

        MutableTrack cleanedTrack = outlierDetector.clean(testTrack).get();

        assertEquals(sizeBeforeCleaning, cleanedTrack.size() + knownOutliers.size());

        assertTrue(
            testTrack.size() == sizeBeforeCleaning - knownOutliers.size(),
            "These original track was mutated"
        );
    }
}
