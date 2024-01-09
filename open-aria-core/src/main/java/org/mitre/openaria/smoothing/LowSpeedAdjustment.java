
package org.mitre.openaria.smoothing;

import static org.mitre.openaria.core.PointField.TIME;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.caasd.commons.DataCleaner;

/**
 * This filter modifies the times around low speed points so that the time windows for regressions
 * elongate over low speed portions of the track.  After the supplied DataCleaner is applied to the
 * adjusted times, the times are reset to their original values. This adjustment is important in
 * cross track filtering in order to get an accurate heading estimate when the aircraft stops
 * moving.
 */
public class LowSpeedAdjustment implements DataCleaner<MutableTrack> {

    final double lowSpeed = 15.0;
    final double stationarySpeed = 1.0;

    final DataCleaner<MutableTrack> cleaner;

    public LowSpeedAdjustment(DataCleaner<MutableTrack> cleaner) {
        this.cleaner = cleaner;
    }

    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        NavigableSet<MutablePoint> points = track.points();

        Map<MutablePoint, Instant> originalTimes = points.stream().collect(Collectors.toMap(x -> x, x -> x.time()));
        Map<MutablePoint, Instant> adjustedTimes = adjustedTimes(points);

        setTimes(points, adjustedTimes);
        Optional<MutableTrack> smoothedTrack = cleaner.clean(track);

        if (smoothedTrack.isPresent()) {
            setTimes(smoothedTrack.get().points(), originalTimes);
            return smoothedTrack;
        } else {
            return Optional.empty();
        }
    }

    private Map<MutablePoint, Instant> adjustedTimes(NavigableSet<MutablePoint> points) {
        Map<MutablePoint, Instant> adjustedTimes = new HashMap<>();

        long currentAdjustedTime = points.first().time().toEpochMilli();
        adjustedTimes.put(points.first(), Instant.ofEpochMilli(currentAdjustedTime));

        for (MutablePoint point : points.tailSet(points.first(), false)) {

            long deltaT = point.time().toEpochMilli() - points.lower(point).time().toEpochMilli();

            Double speedInKnots = point.speedInKnots();
            double speedWeight = 1.0;
            if (speedInKnots != null) {
                speedWeight = 1.0 - Math.cos(Math.PI / 2.0 * Math.min(1.0, Math.max(stationarySpeed, speedInKnots) / lowSpeed));
            }

            currentAdjustedTime += (long) (deltaT * speedWeight);
            adjustedTimes.put(point, Instant.ofEpochMilli(currentAdjustedTime));
        }
        return adjustedTimes;
    }

    private void setTimes(NavigableSet<MutablePoint> points, Map<MutablePoint, Instant> times) {
        for (MutablePoint point : points) {
            point.set(TIME, times.get(point));
        }
    }
}
