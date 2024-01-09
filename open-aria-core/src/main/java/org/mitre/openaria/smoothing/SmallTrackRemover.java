
package org.mitre.openaria.smoothing;

import java.util.Optional;

import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;

/**
 * A SmallTrackRemover "smooths" a Track by removing Tracks with fewer than n point.
 */
public class SmallTrackRemover implements DataCleaner<Track> {

    private final int MIN_NUM_TRACK_POINTS;

    /**
     * Create a new SmallTrackRemover
     *
     * @param minNumPointsAllowable The minimum number of points an "acceptable" Track can have
     */
    public SmallTrackRemover(int minNumPointsAllowable) {
        this.MIN_NUM_TRACK_POINTS = minNumPointsAllowable;
    }

    @Override
    public Optional<Track> clean(Track track) {
        return (trackIsSmall(track))
            ? Optional.empty()
            : Optional.of(track);
    }

    /**
     * @param track An input track
     *
     * @return True if the track contains fewer than the minimum number of acceptable points
     */
    public boolean trackIsSmall(Track track) {
        return track.size() < MIN_NUM_TRACK_POINTS;
    }
}
