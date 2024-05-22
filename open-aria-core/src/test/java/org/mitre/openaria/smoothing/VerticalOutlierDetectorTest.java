
package org.mitre.openaria.smoothing;

import static java.util.stream.Collectors.toCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.nop.NopEncoder;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.smoothing.VerticalOutlierDetector.AnalysisResult;

import org.junit.jupiter.api.Test;

public class VerticalOutlierDetectorTest {

    @Test
    public void testNoOutlier() {
        /*
         * Confirm that a flat altitude profile with a single 100 foot deviation (i.e. the minimum
         * possible altitude change) does not create an outlier.
         */
        Track<NopHit> testTrack2 = createTrackFromResource(VerticalOutlierDetector.class, "NoAltitudeOutlier_1.txt");

        Collection<AnalysisResult<NopHit>> outliers = (new VerticalOutlierDetector<NopHit>()).getOutliers(testTrack2);

        assertTrue(
            outliers.isEmpty(),
            "There should be no outliers, the test data contains the minimum possible altitude change"
        );
    }

    @Test
    public void testNoOutlier_smoothing() {
        /*
         * Confirm that a track with no vertical outliers does not change due to smoothing
         */
        Track<NopHit> testTrack = createTrackFromResource(
            VerticalOutlierDetector.class,
            "NoAltitudeOutlier_1.txt"
        );

        Collection<AnalysisResult<NopHit>> outliers = (new VerticalOutlierDetector<NopHit>()).getOutliers(testTrack);

        assertTrue(outliers.isEmpty());

        Track<NopHit> postSmoothing = (new VerticalOutlierDetector<NopHit>()).clean(testTrack).get();

        Iterator<Point<NopHit>> iter1 = testTrack.points().iterator();
        Iterator<Point<NopHit>> iter2 = postSmoothing.points().iterator();

        while (iter1.hasNext() && iter2.hasNext()) {
            Point nextFrom1 = iter1.next();
            Point nextFrom2 = iter2.next();

            assertEquals(
                nextFrom1,
                nextFrom2
            );
        }

        //both iterations should end at the same time.
        assertFalse(iter1.hasNext());
        assertFalse(iter2.hasNext());
    }

    @Test
    public void testNoOutlier_2() {
        /*
         * Confirm that a flat altitude profile with a single 300 foot deviation (i.e. 3 times the
         * minimum possible altitude change) does not create an outlier.
         */
        Track<NopHit> testTrack2 = createTrackFromResource(VerticalOutlierDetector.class, "NoAltitudeOutlier_2.txt");

        Collection<AnalysisResult<NopHit>> outliers = (new VerticalOutlierDetector<NopHit>()).getOutliers(testTrack2);

        assertTrue(
            outliers.isEmpty(),
            "There should be no outliers, the test data contains the minimum possible altitude change"
        );
    }

    @Test
    public void testMinimumOutlier() {
        /*
         * Confirm that a flat altitude profile with a single 400 foot deviation (i.e. 4 times the
         * minimum possible altitude change) DOES create an outlier.
         */
        Track<NopHit> testTrack2 = createTrackFromResource(VerticalOutlierDetector.class, "MinimumAltitudeOutlier.txt");

        Collection<AnalysisResult<NopHit>> outliers = (new VerticalOutlierDetector<NopHit>()).getOutliers(testTrack2);

        confirmExactlyTheseOutliers(
            outliers,
            "[RH],AGW,RDG,09/20/2017,17:28:02.096,,,,2525,204,425,252,040.49450,-075.76505,110,,10.66,5.09,,,,RDG,,,,,???,,,,,4221,???,,00,,,1,,0,,90.31,88.64,{RH}"
        );
    }

    @Test
    public void testMinimumOutlier_smoothing() {
        /*
         * Confirm that smoothing (1) does not reduce the number of point, (2) produces a track with
         * no vertical outliers
         */
        Track<NopHit> testTrack = createTrackFromResource(
            VerticalOutlierDetector.class,
            "MinimumAltitudeOutlier.txt"
        );

        int sizeBeforeSmoothing = testTrack.size(); //

        VerticalOutlierDetector<NopHit> vod = new VerticalOutlierDetector<>();

        Collection<AnalysisResult<NopHit>> outliersBeforeSmoothing = vod.getOutliers(testTrack);

        assertFalse(outliersBeforeSmoothing.isEmpty(), "Outliers should have been detected");

        Track<NopHit> trackAfterSmoothing = vod.clean(testTrack).get();

        Collection<AnalysisResult<NopHit>> outliersAfterSmoothing = vod.getOutliers(trackAfterSmoothing);

        assertTrue(outliersAfterSmoothing.isEmpty(), "Ouliers should have been removed");
        assertEquals(sizeBeforeSmoothing, trackAfterSmoothing.size());
    }

    @Test
    public void testMissingAltitude_aka_modeCSwap_1() {
        /*
         * Some NOP data contains "mode C swaps". Mode C swaps occur when one aircraft (say at 20k
         * feet) flies directly over top of another aircraft (at say 10k feet). When this occurs the
         * NOP system can get confused and drop altitude measurements.
         */
        Track<NopHit> testTrack1 = createTrackFromResource(VerticalOutlierDetector.class, "AltitudeOutlier_1.txt");

        Collection<AnalysisResult<NopHit>> outliers = (new VerticalOutlierDetector<NopHit>()).getOutliers(testTrack1);

        confirmExactlyTheseOutliers(
            outliers,
            "[RH],AGW,RDG,09/20/2017,17:28:02.096,,,,2525,000,425,252,040.49450,-075.76505,110,,10.66,5.09,,,,RDG,,,,,???,,,,,4221,???,,00,,,1,,0,,90.31,88.64,{RH}"
        );
    }

    @Test
    public void testMissingAltitude_aka_modeCSwap_2() {
        /*
         * This is another mode C swap test.
         *
         * This test contains a much much smaller change (from 1k to 0 instead of from 22k to 0).
         * This test data also has a missing altitude value at the very front of the track.
         */

        Track<NopHit> testTrack2 = createTrackFromResource(VerticalOutlierDetector.class, "AltitudeOutlier_2.txt");

        Collection<AnalysisResult<NopHit>> outliers = (new VerticalOutlierDetector<NopHit>()).getOutliers(testTrack2);

        confirmExactlyTheseOutliers(
            outliers,
            "[RH],AGW,ERI_B,09/20/2017,17:36:47.160,,,,1200,000,84,247,042.22935,-079.88024,106,,14.12,10.6,,,,ERI,,,,,???,,,,,4277,???,,00,,,1,,0,,95.17,90.78,{RH}",
            "[RH],AGW,ERI_B,09/20/2017,17:39:00.896,,,,1200,000,87,246,042.20291,-079.94142,106,,11.19,9.45,,,,ERI,,,,,???,,,,,5502,???,,00,,,1,,0,,92.47,89.19,{RH}"
        );
    }

    @Test
    public void testMissingAltitude_aka_modeCSwap_2_smoothing() {
        /*
         * This is another mode C swap test.
         *
         * This test contains a much much smaller change (from 1k to 0 instead of from 22k to 0).
         * This test data also has a missing altitude value at the very front of the track.
         */
        Track<NopHit> testTrack = createTrackFromResource(
            VerticalOutlierDetector.class,
            "AltitudeOutlier_2.txt"
        );

        VerticalOutlierDetector<NopHit> vod = new VerticalOutlierDetector<>();

        assertFalse(vod.getOutliers(testTrack).isEmpty(), "There should be outliers before smoothing");

        Track<NopHit> smoothedTrack = vod.clean(testTrack).get();

        assertTrue(vod.getOutliers(smoothedTrack).isEmpty(), "There should be no outliers after smoothing");
    }

    @Test
    public void testBuggedOutlier() {
        /*
         * This track contains a "outlier" that shouldn't really be an outlier. This test ensure
         * that future editions of VerticalOutlierDetector do not flag this track.
         */
        Track<NopHit> testTrack = createTrackFromResource(
            VerticalOutlierDetector.class,
            "outlierBug-A11-HAG75A-7.txt"
        );

        /*
         * An imperfect outlier detector can consider this point an Outlier:
         *
         * [RH],STARS,A11,10/18/2016,02:38:16.518,,,,2261,213,217,110,061.36202,-153.26353,0038,0000,-93.3379,13.3411,,,,A11,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}
         */
        Collection<AnalysisResult<NopHit>> outliers = (new VerticalOutlierDetector<NopHit>()).getOutliers(testTrack);

        confirmExactlyTheseOutliers(
            outliers,
            "[RH],STARS,A11,10/18/2016,02:25:16.460,,,,2261,000,000,xxx,061.64543,-154.94138,0038,0000,-140.2090,33.3177,,,,A11,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}"
        );

    }

    private void confirmExactlyTheseOutliers(Collection<AnalysisResult<NopHit>> foundOutliers, String... expectedOutliters) {

        NopEncoder nopEncoder = new NopEncoder();

        Set<String> knownOutliers = Stream.of(expectedOutliters)
            .map(asRawNop -> NopHit.from(asRawNop)) //convert the raw Nop to Points
            .map(p -> nopEncoder.asRawNop(p)) //get the "lossy" Strings
            .collect(toCollection(HashSet::new)); //in a HashSet

        Collection<Point<NopHit>> outlieingPoints = foundOutliers.stream()
            .map(analysisResult -> analysisResult.originalPoint())
            .toList();

        assertThat(foundOutliers.size(), is(expectedOutliters.length));

        for (Point<NopHit> foundOutlier : outlieingPoints) {
            assertTrue(knownOutliers.contains(nopEncoder.asRawNop(foundOutlier)));
        }

        assertEquals(
            foundOutliers.size(), expectedOutliters.length,
            "The number of found outliers must match the number of expected outliers"
        );
    }
}
