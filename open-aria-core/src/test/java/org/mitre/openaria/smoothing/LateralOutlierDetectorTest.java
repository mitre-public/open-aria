
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;
import static org.mitre.openaria.core.Tracks.createTrackFromFile;

import java.io.File;
import java.time.Duration;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.Set;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Speed;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.nop.NopEncoder;
import org.mitre.openaria.core.formats.nop.NopHit;

import org.junit.jupiter.api.Test;


public class LateralOutlierDetectorTest {

    @Test
    public void testFindingOneOutlier() {

        //this point is the lateral outlier if the test data
        Point<NopHit> theOutlier = NopHit.from("[RH],STARS,D21_B,03/24/2018,14:57:02.226,N518SP,C172,,5256,031,109,184,042.46462,-083.75121,3472,5256,-16.5222,15.1222,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}");

        Track<NopHit> testTrack = createTrackFromFile(
            new File("src/test/resources/oneLateralOutlierTest.txt")
        );

        NavigableSet<Point<NopHit>> outliers = (new LateralOutlierDetector<NopHit>()).getOutliers(testTrack);

        assertEquals(1, outliers.size());
        assertEquals(outliers.first().time(), theOutlier.time());

        int sizeBeforeCleaning = testTrack.size();
        Track<NopHit> cleanedTrack = (new LateralOutlierDetector<NopHit>()).clean(testTrack).get();

        assertEquals(sizeBeforeCleaning, cleanedTrack.size() + 1);
        assertNotEquals(cleanedTrack.nearestPoint(theOutlier.time()).time(), theOutlier.time());
    }

    @Test
    public void testCurvyTrack() {

        //this test track is nice and smooth -- it shouldn't flag any outliers (even in the curves)
        Track<NopHit> testTrack = createTrackFromFile(new File("src/test/resources/curvyTrack.txt"));

        NavigableSet<Point<NopHit>> outliers = (new LateralOutlierDetector<NopHit>()).getOutliers(testTrack);

        assertEquals(0, outliers.size());

        Track<NopHit> cleanedTrack = (new LateralOutlierDetector<NopHit>()).clean(testTrack).get();

        assertEquals(testTrack.size(), cleanedTrack.size());
    }

    @Test
    public void testNearPerfectTrackWithMinorJitter() {

        Track<String> testTrack = trackWithVeryMinorJitter();

        LateralOutlierDetector<String> outlierDetector = new LateralOutlierDetector<>();

        NavigableSet<Point<String>> outliers = outlierDetector.getOutliers(testTrack);
        assertEquals(0, outliers.size());

        Track<String> cleanedTrack = outlierDetector.clean(testTrack).get();

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
    public static Track<String> trackWithVeryMinorJitter() {

        LinkedList<Point<String>> points = newLinkedList();

        Point<String> cur = Point.<String>builder().latLong(10.0, 10.0).time(EPOCH).build();
        points.add(cur);

        int NUM_POINTS = 30;
        double NORTH_EAST = 45.0;
        double NORTH_WEST = 360 - 45;
        Duration timeStep = Duration.ofSeconds(5);
        Distance dist = Speed.of(200, KNOTS).times(timeStep);

        while (points.size() < NUM_POINTS) {
            Point<String> last = points.getLast();
            Point<String> next = Point.<String>builder()
                .latLong(last.latLong().projectOut(NORTH_EAST, dist.inNauticalMiles()))
                .time(last.time().plus(timeStep))
                .build();
            points.add(next);
        }

        //manipulate the 15th point very slightly
        Point<String> prior = points.get(14);
        LatLong newPosition = prior.latLong().projectOut(NORTH_WEST, 0.1); //adjust location 0.01 NM
        Point<String> adjustedPoint = Point.builder(prior).latLong(newPosition).build();
        points.set(14, adjustedPoint);

        return Track.of(points);
    }

    @Test
    public void testRealTrackWithGentalError() {

        // NOT GETTING OUTLIER AT: 21:15:25
        //THESE ARE REASONABLE OUTLIERS
        String outlier1 = "[RH],STARS,ABE_B,03/25/2018,21:39:54.596,N317A,SR22,,0224,032,161,267,040.49013,-075.64892,2326,0224,-9.5386,-9.7261,1,D,A,ABE,RDG,PTW,ABE,2114,ABE,ACT,VFR,,00957,,,,,,S,1,,0,{RH}";
        String outlier2 = "[RH],STARS,ABE_B,03/25/2018,21:45:18.207,N317A,SR22,,0224,033,170,269,040.48656,-075.97119,2326,0224,-24.2925,-9.8784,0,4,A,ABE,RDG,PTW,ABE,2114,ABE,ACT,VFR,,00957,,,,,,S,1,,0,{RH}";

        Set<String> knownOutliers = newHashSet(
            outlier1,
            outlier2
        );

        Track<NopHit> testTrack = createTrackFromFile(
            new File("src/test/resources/trackWithSomeGentalError.txt"));

        LateralOutlierDetector<NopHit> outlierDetector = new LateralOutlierDetector<>();

        NavigableSet<Point<NopHit>> outliers = outlierDetector.getOutliers(testTrack);

        assertEquals(2, outliers.size(), "We found exactly 2 outliers");

        NopEncoder nopEncoder = new NopEncoder();

        //and they are the two shown above
        for (Point<?> outlier : outliers) {
            assertTrue(knownOutliers.contains(nopEncoder.asRawNop(outlier)));
        }

        //using the outlierDetector's "clean" method will mutate the input, save the prior state
        int sizeBeforeCleaning = testTrack.size();

        Track<NopHit> cleanedTrack = outlierDetector.clean(testTrack).get();

        assertEquals(sizeBeforeCleaning, cleanedTrack.size() + knownOutliers.size());

        assertThat(testTrack.size(), is(cleanedTrack.size() + knownOutliers.size()));
    }
}
