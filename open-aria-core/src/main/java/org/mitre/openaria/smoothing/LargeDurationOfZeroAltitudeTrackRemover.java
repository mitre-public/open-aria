package org.mitre.openaria.smoothing;

import static org.mitre.caasd.commons.util.Partitioners.newTreeSetCollector;

import java.time.Duration;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;

/**
 * Removes tracks from further processing that contain too many consecutive null altitudes.
 * <p>
 * This is really only useful for surveillance sources that do not expect 0 altitudes (i.e.,
 * non-surface).
 */
public class LargeDurationOfZeroAltitudeTrackRemover implements DataCleaner<Track> {

    private static final Predicate<Point> missingAltitudes = pt -> pt.altitude() == null || pt.altitude().inFeet() == 0;

    /**
     * Remove tracks which have zero altitudes for longer than this duration.
     */
    private final Duration maxDurationAllowed;

    public LargeDurationOfZeroAltitudeTrackRemover(Duration maxDurationAllowed) {
        this.maxDurationAllowed = maxDurationAllowed;
    }

    @Override
    public Optional<Track> clean(Track track) {
        return hasZeroAltitudeLongerThanAllowed(track) ?
            Optional.empty() :
            Optional.of(track);
    }

    protected boolean hasZeroAltitudeLongerThanAllowed(Track track) {

        NavigableSet<? extends Point> points = track.points();
        List<? extends TreeSet<? extends Point>> gapsOfZeroAltitudes = partitionByZeroAltitudes(points);

        return gapsOfZeroAltitudes
            .stream()
            .map(this::durationOfGap)
            .anyMatch(d -> d.compareTo(maxDurationAllowed) > 0);
    }

    /**
     * Returns the points with zero altitude grouped in succession.
     */
    protected List<? extends TreeSet<? extends Point>> partitionByZeroAltitudes(NavigableSet<? extends Point> points) {
        return points.stream()
            .collect(newTreeSetCollector(missingAltitudes))
            .stream()
            .filter(ts -> missingAltitudes.test(ts.first()))
            .collect(Collectors.toList());
    }

    private Duration durationOfGap(NavigableSet<? extends Point> gap) {
        return Duration.between(gap.first().time(), gap.last().time());
    }
}
