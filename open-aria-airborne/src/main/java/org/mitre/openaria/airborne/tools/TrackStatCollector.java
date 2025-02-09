package org.mitre.openaria.airborne.tools;

import java.util.function.Consumer;

import org.mitre.openaria.core.Track;

import com.google.common.math.Stats;
import com.google.common.math.StatsAccumulator;

/**
 * TrackStatCollector harvests summary statistics on: Track size (e.g. point count), Track
 * duration (in seconds), and Track Points per Minutes
 */
public class TrackStatCollector implements Consumer<Track> {

    // Helps us compute min/avg/max/std/var of track.size()
    StatsAccumulator trackSizeAccumulator = new StatsAccumulator();

    // Helps us compute min/avg/max/std/var of track.lengthInSec()
    StatsAccumulator trackDurationAccumulator = new StatsAccumulator();

    StatsAccumulator pointsPerMinuteAccumulator = new StatsAccumulator();

    @Override
    public void accept(Track track) {
        trackSizeAccumulator.add(track.size());
        trackDurationAccumulator.add(track.asTimeWindow().duration().getSeconds());

        if (track.asTimeWindow().duration().getSeconds() > 0) {
            double trackDurMin = track.asTimeWindow().duration().getSeconds() / 60.0;
            double ptsPerMinute = track.size() / trackDurMin;
            pointsPerMinuteAccumulator.add(ptsPerMinute);
        }
    }

    public Stats trackDurationStats() {
        return trackDurationAccumulator.snapshot();
    }

    public Stats trackSizeStats() {
        return trackSizeAccumulator.snapshot();
    }

    public Stats pointsPerMinuteStats() {
        return pointsPerMinuteAccumulator.snapshot();
    }
}
