
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;

/**
 * A TrackPairCleaner applies a {@literal DataCleaner<Track>} to both Tracks in a TrackPair.
 */
public class TrackPairCleaner implements DataCleaner<TrackPair> {

    private final DataCleaner<Track> trackSmoother;

    public TrackPairCleaner(DataCleaner<Track> trackCleaner) {
        this.trackSmoother = checkNotNull(trackCleaner);
    }

    /**
     * Clean both the Tracks in this TrackPair using the {@literal DataCleaner<Track>} provided at
     * construction.
     *
     * @param trackPair A pair of tracks that need need to be cleaned.
     *
     * @return An Optional TrackPair if and only if both Track in the original Pair would cleaned
     *     successfully.
     */
    @Override
    public Optional<TrackPair> clean(TrackPair trackPair) {
        checkNotNull(trackPair, "The input track pair is null");
        checkNotNull(trackPair.track1(), "trackPair.first() is null");
        checkNotNull(trackPair.track2(), "trackPair.second() is null");

        Optional<Track> smoothedFirst = trackSmoother.clean(trackPair.track1());
        Optional<Track> smoothedSecond = trackSmoother.clean(trackPair.track2());

        boolean bothPresent = (smoothedFirst.isPresent() && smoothedSecond.isPresent());

        return (bothPresent)
            ? Optional.of(TrackPair.of(smoothedFirst.get(), smoothedSecond.get()))
            : Optional.empty();
    }

    public static TrackPairCleaner from(DataCleaner<Track> trackCleaner) {
        return new TrackPairCleaner(trackCleaner);
    }
}
