

package org.mitre.openaria.smoothing;

import java.time.Duration;

import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.CompositeCleaner;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.ExceptionCatchingCleaner;
import org.mitre.caasd.commons.Functions.ToStringFunction;
import org.mitre.caasd.commons.util.ExceptionHandler;
import org.mitre.caasd.commons.util.SequentialFileWriter;

public class TrackSmoothing {

    /**
     * @return A single DataCleaner built from a chain of DataCleaners that are run in sequence.
     */
    public static DataCleaner<Track> coreSmoothing() {
        return CompositeCleaner.of(
            //removes error prone synthetic "assumed" points from Nop data
            new CoastedPointRemover(),
            //remove both points if any two sequential points are within 500 Milliseconds
            new HighFrequencyPointRemover(Duration.ofMillis(500)),
            //remove tracks with small just a handful of points,
            new SmallTrackRemover(9),
            mutableTrackSmoothing()
        );
    }

    /*
     * These smoothing operations are FAR more efficient when they can directly edit the input Track
     * data. Consequently, these smoothers operate on MutableTracks and not immutable Tracks.
     */
    private static DataCleaner<Track> mutableTrackSmoothing() {
        return MutableSmoother.of(
            /*
             * ensure any two sequential points have at least 4 seconds between them (by removing
             * only the trailing points)
             */
            new TimeDownSampler(Duration.ofMillis(4_000)),
            //correct missing speed values
            new FillMissingSpeeds(),
            //removes near-stationary Tracks produces by "radar mirages" off of skyscrapers and such
            new RemoveLowVariabilityTracks(),
            //removes near-duplicate points when a track is stationary.
            new DistanceDownSampler(),
            //forces 000 altitudes to null
            new ZeroAltitudeToNull(),
            //correct missing altitude values
            new FillMissingAltitudes(),
            //correct the altitude values for outlying Points
            new VerticalOutlierDetector(),
            //remove points with inconsistent LatLong values
            new LateralOutlierDetector(),
            //remove radar noise using polynomial fitting
            new TrackFilter()
        );
    }

    public static DataCleaner<Track> simpleSmoothing() {

        DataCleaner<Track> cleaner = coreSmoothing();
        ToStringFunction<Track> toString = track -> track.asNop();
        ExceptionHandler exceptionHandler = new SequentialFileWriter("trackCleaningExceptions");

        return new ExceptionCatchingCleaner<>(cleaner, toString, exceptionHandler);
    }
}
