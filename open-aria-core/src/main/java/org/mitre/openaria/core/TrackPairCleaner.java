
package org.mitre.openaria.core;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.mitre.caasd.commons.DataCleaner;

/**
 * A TrackPairCleaner applies a {@literal DataCleaner<Track>} to both Tracks in a TrackPair.
 */
public class TrackPairCleaner implements DataCleaner<TrackPair> {

    private final DataCleaner<Track> trackSmoother;

    public TrackPairCleaner(DataCleaner<Track> trackCleaner) {
        this.trackSmoother = requireNonNull(trackCleaner);
    }

    /**
     * Clean both the Tracks in this TrackPair using the {@literal DataCleaner<Track>} provided at
     * construction.
     *
     * @param trackPair A pair of tracks that will be cleaned.
     *
     * @return An Optional TrackPair if and only if both Tracks in the original Pair are cleaned
     *     successfully.
     */
    @Override
    public Optional<TrackPair> clean(TrackPair trackPair) {
        requireNonNull(trackPair, "The input track pair is null");
        requireNonNull(trackPair.track1(), "trackPair.first() is null");
        requireNonNull(trackPair.track2(), "trackPair.second() is null");

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
