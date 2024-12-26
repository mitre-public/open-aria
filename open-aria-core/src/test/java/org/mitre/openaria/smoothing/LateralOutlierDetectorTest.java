
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.caasd.commons.Speed.Unit.KNOTS;
import static org.mitre.caasd.commons.maps.MapFeatures.filledCircle;
import static org.mitre.openaria.core.Tracks.createTrackFromFile;

import java.awt.Color;
import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.maps.MapBuilder;
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

        Distance SHIFT_60_FT = Distance.ofNauticalMiles(0.01);
        Track<String> testTrack = trackWithJitter(SHIFT_60_FT);

        LateralOutlierDetector<String> outlierDetector = new LateralOutlierDetector<>();

        NavigableSet<Point<String>> outliers = outlierDetector.getOutliers(testTrack);
        assertEquals(0, outliers.size());

        Track<String> cleanedTrack = outlierDetector.clean(testTrack).get();

        assertEquals(testTrack.size(), cleanedTrack.size());
    }

    @Test
    public void testNearPerfectTrackWithMajorJitter() {

        Distance SHIFT_600_FT = Distance.ofNauticalMiles(0.1);
        Track<String> testTrack = trackWithJitter(SHIFT_600_FT);

        LateralOutlierDetector<String> outlierDetector = new LateralOutlierDetector<>();

        NavigableSet<Point<String>> outliers = outlierDetector.getOutliers(testTrack);
        assertEquals(1, outliers.size());

        Track<String> cleanedTrack = outlierDetector.clean(testTrack).get();

        assertEquals(testTrack.size(), cleanedTrack.size() + 1);
    }


    /**
     * @return A Track with points that travel in a near perfect straight line. One of the points in
     *     this track was shifted by a few feet. This single point breaks the perfect pattern
     *     despite being a very small distance of "perfect". This test case is designed to catch
     *     situations in which a zero error "degrades" to a ridiculously tiny error (i.e. no outlier
     *     should be detected even though the error increased "dramatically" (when compared to zero
     *     error))
     */
    public static Track<String> trackWithJitter(Distance shiftDistance) {

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
        LatLong newPosition = prior.latLong().projectOut(NORTH_WEST, shiftDistance.inNauticalMiles()); //adjust location 0.01 NM
        Point<String> adjustedPoint = Point.builder(prior).latLong(newPosition).build();
        points.set(14, adjustedPoint);

        return Track.of(points);
    }


    @Test
    public void testRealTrackWithGentleError() {

        Track<NopHit> testTrack = createTrackFromFile(
            new File("src/test/resources/trackWithSomeGentleError.txt"));

        //THESE ARE REASONABLE OUTLIERS
        String outlier1 = "[RH],STARS,ABE_B,03/25/2018,21:18:06.882,N317A,SR22,,0224,031,156,341,040.34347,-075.32023,2326,0224,5.5200,-18.5269,1,D,A,ABE,G06,PTW,ABE,2114,ABE,ACT,VFR,,00957,,,,,,S,1,,0,{RH}";
        String outlier2 = "[RH],STARS,ABE_B,03/25/2018,21:18:54.901,N317A,SR22,,0224,030,157,340,040.37678,-075.33763,2326,0224,4.7192,-16.5308,1,D,A,ABE,G06,PTW,ABE,2114,ABE,ACT,IFR,,00957,,,,,,S,1,,0,{RH}";
        String outlier3 = "[RH],STARS,ABE_B,03/25/2018,21:30:30.911,N317A,SR22,,0224,017,109,053,040.60665,-075.51508,2326,0224,-3.4058,-2.7495,1,A,A,ABE,RDG,PTW,ABE,2114,ABE,ACT,IFR,,00957,,,,,,S,1,,0,{RH}";
        String outlier4 = "[RH],STARS,ABE_B,03/25/2018,21:38:18.748,N317A,SR22,,0224,031,163,225,040.50022,-075.55797,2326,0224,-5.3745,-9.1284,1,D,A,ABE,RDG,PTW,ABE,2114,ABE,ACT,VFR,,00957,,,,,,S,1,,0,{RH}";
        String outlier5 = "[RH],STARS,ABE_B,03/25/2018,21:39:54.596,N317A,SR22,,0224,032,161,267,040.49013,-075.64892,2326,0224,-9.5386,-9.7261,1,D,A,ABE,RDG,PTW,ABE,2114,ABE,ACT,VFR,,00957,,,,,,S,1,,0,{RH}";

        Set<String> knownOutliers = newHashSet(
            outlier1, outlier2, outlier3, outlier4, outlier5
        );

        LateralOutlierDetector<NopHit> outlierDetector = new LateralOutlierDetector<>();

        NavigableSet<Point<NopHit>> foundOutliers = outlierDetector.getOutliers(testTrack);

//        plotOnMap(testTrack, foundOutliers, 11);

        assertThat(foundOutliers, hasSize(5));

        NopEncoder nopEncoder = new NopEncoder();
        for (Point<?> outlier : foundOutliers) {
            assertTrue(knownOutliers.contains(nopEncoder.asRawNop(outlier)));
        }

        int sizeBeforeCleaning = testTrack.size();

        Track<NopHit> cleanedTrack = outlierDetector.clean(testTrack).get();

        assertThat(sizeBeforeCleaning, is(cleanedTrack.size() + knownOutliers.size()));
    }

    @Test
    void testOutlierGetsRemoved() {
        Track<NopHit> track = createTrackFromFile(
            new File("src/test/resources/outlier_track_DAL2817_ASA589.txt")
        );

        // These two points are SERIOUS errors.
        // Previous outlier detection methods permitted these points to "come through"

        // This point is a "stagnant" point where the same LatLong is used to at 2 timestamps even when the aircraft is moving a 300knots
        String requiredOutlier_1 = "[RH],Center,ZSE_P,12-05-2024,23:05:13.000,DAL2817,B738,L,6030,360,431,315,42.6639,-114.5178,753,,,,,ZLC/41,,ZSE_P,,,,E2335,SEA,,IFR,,753,35232634,SLC,0016,360//360,,L,1,,,{RH}";
        // This point is a "shadow" where the LatLong from 50 seconds in the past "reappears" and the aircraft jumps forward
        String requiredOutlier_2 = "[RH],Center,ZSE_P,12-05-2024,23:57:29.000,DAL2817,B738,L,6030,286,435,305,46.8136,-120.9567,753,,,,,ZSE/31,,ZSE_P,,,,E2335,SEA,,IFR,,753,35267836,SLC,0016,286/120/240,,L,1,,,{RH}";

        Set<String> knownOutliers = newHashSet(requiredOutlier_1, requiredOutlier_2);

        LateralOutlierDetector<NopHit> detector = new LateralOutlierDetector<>();
        NavigableSet<Point<NopHit>> foundOutliers = detector.getOutliers(track);

        assertThat(foundOutliers.size(), greaterThanOrEqualTo(2));

        NopEncoder nopEncoder = new NopEncoder();
        List<String> foundAsStr = foundOutliers.stream().map(out -> nopEncoder.asRawNop(out)).toList();

        knownOutliers.forEach(
            known -> assertThat(foundAsStr.contains(known), is(true))
        );

//        plotOnMap(track, foundOutliers, 8);
    }


    @Test
    void testOutlierGetsRemoved_2() {
        Track<NopHit> track = createTrackFromFile(
            new File("src/test/resources/outlier_track_FFT1876.txt")
        );

        // These three points are SERIOUS errors.
        // Previous outlier detection methods permitted these points to "come through"

        // CLEAR DATA ERROR
        // This point is a "shadow" where the LatLong from 36 seconds in the past "reappears" and the aircraft jumps backward
        String requiredOutlier_1 = "[RH],Center,ZLA_P,12-07-2024,08:06:38.000,FFT1876,A321,L,1032,049,229,086,36.1333,-115.0831,808,,,,,/,,ZLA_P,,,,D0804,MCO,,IFR,,808,70396930,LAS,1200,049/190/350,,L,1,,,{RH}";

        // CLEAR DATA ERROR (twin points, possible partial update)
        // This point is part of a pair of points with the same timestamp.
        // This point contains the LatLong from 12 seconds ago ... so its LatLong location doesn't fit the regression
        String requiredOutlier_2 = "[RH],Center,ZLA_P,12-07-2024,08:10:26.000,FFT1876,A321,L,1032,129,312,102,36.1133,-114.6994,808,,,,,ZLA/08,,ZLA_P,,,,D0804,MCO,,IFR,,808,70397875,LAS,1200,129//350,,L,1,,,{RH}";

        // CLEAR DATA ERROR -- Monstrously wrong!
        // This point is a "shadow" where the LatLong from 36 seconds in the past "reappears" and the aircraft jumps backward
        // But here the aircraft if flying 446 knots, so the size of the jump is HUGE
        String requiredOutlier_3 = "[RH],Center,ZLA_P,12-07-2024,08:27:14.000,FFT1876,A321,L,1032,338,446,100,35.7772,-112.3756,808,,,,,ZLA/08,,ZLA_P,,,,E0815,MCO,,IFR,,808,70401634,LAS,1200,338//350,,L,1,,,{RH}";

        Set<String> knownOutliers = newHashSet(requiredOutlier_1, requiredOutlier_2, requiredOutlier_3);

        LateralOutlierDetector<NopHit> detector = new LateralOutlierDetector<>();
        NavigableSet<Point<NopHit>> foundOutliers = detector.getOutliers(track);

        assertThat(foundOutliers.size(), greaterThanOrEqualTo(3));

        NopEncoder nopEncoder = new NopEncoder();
        List<String> foundAsStr = foundOutliers.stream().map(out -> nopEncoder.asRawNop(out)).toList();

        knownOutliers.forEach(
            known -> assertThat(foundAsStr.contains(known), is(true))
        );

//        plotOnMap(track, foundOutliers, 8);
    }

    @Test
    void investigateOutliers() {

        // The purpose of this code is to visually inspect the application of the outlier detector
        // on ONE of the tracks from "scaryTrackData.txt".  "outlier_track_test.txt" = contains one
        // of the tracks from this file.

        // This test exists because confusion arose when processing "scaryTrackData.txt" DID NOT
        // find outliers.  The reason this LateralOutlierDetector did not find outliers was that a
        // different DataCleaner removed those problematic points (at first I thought I had
        // uncovered an issue ... thankfully that was not the case)

        Track<NopHit> track = createTrackFromFile(new File("src/test/resources/outlier_track_test.txt"));

        LateralOutlierDetector<NopHit> detector = new LateralOutlierDetector<>();
        NavigableSet<Point<NopHit>> foundOutliers = detector.getOutliers(track);

        // Visual inspection will show 6 clear outliers
        assertThat(foundOutliers, hasSize(6));
//        plotOnMap(track, foundOutliers, 11);
    }

    void plotOnMap(Track<NopHit> track, Collection<Point<NopHit>> outliers, int zoomLevel) {

        LatLong avgLocation = LatLong.avgLatLong(track.points().stream().map(pt -> pt.latLong()).toList());

        new MapBuilder()
            .solidBackground(Color.BLACK)
            .width(3600, zoomLevel)
            .center(avgLocation)
            .addFeatures(track.points(), pt -> filledCircle(pt.latLong(), Color.BLUE, 16))
            .addFeatures(outliers, pt -> filledCircle(pt.latLong(), Color.RED, 16))
            .toFile(new File("trackOutliers.png"));
    }
}
