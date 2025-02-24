
package org.mitre.openaria.core;

import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.Tracks.hasAircraftId;

import java.util.Optional;

import org.mitre.caasd.commons.TimeWindow;

/**
 * This class contains convenience methods for manipulating TrackPairs.
 */
public class TrackPairs {

    /**
     * @param t1 The first Track
     * @param t2 The second Track
     *
     * @return True if the data from these two tracks overlaps in time.
     */
    public static boolean overlapInTime(Track<?> t1, Track<?> t2) {
        requireNonNull(t1);
        requireNonNull(t2);
        return t1.asTimeWindow().overlapsWith(t2.asTimeWindow());
    }

    /**
     * @param t1 The first Track
     * @param t2 The second Track
     *
     * @return The TimeWindow that covers the space of time for which there is data for both tracks
     *     space if the data from these two tracks overlap in time.
     */
    public static <T> Optional<TimeWindow> timeOverlap(Track<T> t1, Track<T> t2) {
        return TrackPair.of(t1, t2).timeOverlap();
    }

    public static <T> boolean atLeastOneTrackHasAircraftId(TrackPair<T> pair) {
        return hasAircraftId(pair.track1()) || hasAircraftId(pair.track2());
    }

    /**
     * Estimate the maximum lateral distance between two tracks at any given moment in time. This
     * computation is only based on lat/long data and time data. In other words, altitude data is
     * ignored. This estimate is computed by creating a synthetic/interpolated "matching point" in
     * track 1 for each Point in track 2 (and vice versa). For example, if a track 1 has a point at
     * time t then a "matching point" on track 2 is generated. This matching point also occurs at
     * time t. The lateral distance between these two points (with perfectly matching time values)
     * is then computed. The result of this method is the maximum value of these distance
     * comparisons.
     *
     * @param pair A Pair of Tracks
     *
     * @return The maximum instantaneous distance between each track (ignores altitude, only uses
     *     Lat/Long position data for this computation)
     */
    public static <T> double maxDistBetween(TrackPair<T> pair) {
        return Tracks.maxDistBetween(pair.track1(), pair.track2());
    }

    /**
     * Returns True if these two tracks are ever separated by a fixed <b>lateral</b> distance (ie
     * altitude data is ignored). This method is much faster than using {@code maxDistBetween(t1,
     * t2) > distInNm} because this method usually returns a result without needing to iterate
     * across the entire track
     *
     * @param t1       A Track
     * @param t2       A Track
     * @param distInNm A distance in Nautical Miles
     *
     * @return True if these two tracks ever separated by this required distance. These tracks must
     *     overlap in time for this method to have any meaning.
     */
    public static <T> boolean separateBy(Track<T> t1, Track<T> t2, double distInNm) {
        return TrackPair.of(t1, t2).separateBy(distInNm);
    }
}
