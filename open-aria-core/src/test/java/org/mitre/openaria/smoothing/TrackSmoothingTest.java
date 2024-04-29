
package org.mitre.openaria.smoothing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mitre.openaria.core.Tracks.createTrackFromResource;
import static org.mitre.openaria.smoothing.RemoveLowVariabilityTracksTest.erroneousTrackFromRadarMirage;
import static org.mitre.openaria.smoothing.TrackSmoothing.coreSmoothing;
import static org.mitre.openaria.smoothing.TrackSmoothing.simpleSmoothing;

import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.Tracks;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TrackSmoothingTest {

    @Test
    public void bug292_generatingNevativeSpeeds() {
        for (String exampleFile : exampleFiles()) {

            Track rawTrackDataThatGeneratesNegativeSpeeds = createTrackFromResource(
                TrackSmoothing.class,
                exampleFile
            );

            DataCleaner<Track> cleaner = coreSmoothing();

            assertDoesNotThrow(
                () -> cleaner.clean(rawTrackDataThatGeneratesNegativeSpeeds)
            );
        }
    }

    private String[] exampleFiles() {
        return new String[]{
            "sampleTrack_bug292.txt",
            "sampleTrack2_bug292.txt",
            "sampleTrack3_bug292.txt",
            "sampleTrack4_bug292.txt"
        };
    }

    @Test
    public void trackSmoothingShouldRemoveMirages() {

        //this garabage track is generated by fake radar returns off of radio towers and skyscrapers
        Track trackFromDataThatDoesntMove = erroneousTrackFromRadarMirage();

        DataCleaner<Track> basicSmoothing = TrackSmoothing.simpleSmoothing();
        Optional<Track> result = basicSmoothing.clean(trackFromDataThatDoesntMove);
        assertFalse(result.isPresent(), "This track should not make it through the filter");
    }

    @Test
    public void testSimultaneousSmoothing() {
        /*
         * The file: starsTrackWithCoastedPoints.txt contains...
         *
         * 2 Coasted Points
         *
         * 1 Dropped Point
         *
         * 1 artifically added vertical outlier (the 43rd point had its altitude set to 000)
         *
         * 1 possible vertical outlier that wasn't fixed (the very first point)
         *
         * This test verifies that (A) the coasted and dropped points are removed, (B) the vertical
         * outlier is fixed.
         */

        Track unSmoothedTrack = Tracks.createTrackFromResource(
            Track.class,
            "starsTrackWithCoastedPoints.txt"
        );

        VerticalOutlierDetector vod = new VerticalOutlierDetector();

        int sizeBeforeSmoothing = unSmoothedTrack.size();
        int numVerticalOutliersBeforeSmoothing = vod.getOutliers(unSmoothedTrack).size();

        Track smoothedTrack = (simpleSmoothing()).clean(unSmoothedTrack).get();

        int sizeAfterSmoothing = smoothedTrack.size();
        int numVerticalOutliersAfterSmoothing = vod.getOutliers(smoothedTrack).size();

        //accounts for the DownSampler that removes poitns that occur within 3.5 sec
        int pointsDroppedDueToBeingToCloseInTime = 16;

        int numPointsRemoved = sizeBeforeSmoothing - sizeAfterSmoothing;
        int numVerticalOutliersCorrected = numVerticalOutliersBeforeSmoothing - numVerticalOutliersAfterSmoothing;

        assertEquals(
            3 + pointsDroppedDueToBeingToCloseInTime, numPointsRemoved,
            "Exactly 3 points should be been removed (2 coasted and 1 dropped)"
        );

        assertEquals(
            1,  numVerticalOutliersCorrected,
            "Exactly 1 poitn should have had its altitude corrected (the 1st point)"
        );
    }

    @Disabled
    @Test
    public void baseLineSmoothing() {

        /*
         * This "test" is actually a demo that prints some "smoothed track data" for making graphics
         */
        Track baseLineTrack = createTrackFromResource(
            TrackSmoothing.class,
            "curvyTrack.txt"
        );

        Optional<Track> result = coreSmoothing().clean(baseLineTrack);

        assertDoesNotThrow(() -> result.get());
    }
}
