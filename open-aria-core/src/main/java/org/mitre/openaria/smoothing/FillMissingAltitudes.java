
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.Interpolate.interpolate;
import static org.mitre.openaria.core.PointField.ALTITUDE;

import java.time.Instant;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.SortedSet;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;

/**
 * This DataCleaner adds altitude data to points in a MutableTrack that do not have altitude data.
 * <p>
 * If the first points of a track are missing altitudes, then they are filled with the first present
 * altitude. If the last points of a track are missing altitudes, then they are filled with the last
 * present altitude. If intermediate points of a track are missing altitudes, then they are
 * interpolated between the previous point and the following point. If all the points in a track are
 * missing altitudes, then the track is removed.
 */
public class FillMissingAltitudes implements DataCleaner<MutableTrack> {

    private final HasNullAltitude hasNullAltitude = new HasNullAltitude();

    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        NavigableSet<MutablePoint> points = track.points();

        Optional<MutablePoint> firstNonNull = firstPointWithAltitude(points);
        if (!firstNonNull.isPresent()) {
            return Optional.empty();
        }
        extrapolateAltitudes(points.headSet(firstNonNull.get()), firstNonNull.get());

        Optional<MutablePoint> gapStart;
        Optional<MutablePoint> gapEnd = firstNonNull;

        while (gapEnd.isPresent()) {

            gapStart = firstPointWithoutAltitude(points.tailSet(gapEnd.get()));

            if (!gapStart.isPresent()) {
                break;
            }
            gapEnd = firstPointWithAltitude(points.tailSet(gapStart.get()));

            if (!gapEnd.isPresent()) {
                extrapolateAltitudes(points.tailSet(gapStart.get()), points.lower(gapStart.get()));
            } else {
                interpolateAltitudes(points.subSet(gapStart.get(), gapEnd.get()), points.lower(gapStart.get()), gapEnd.get());
            }
        }

        return Optional.of(MutableTrack.of(points));
    }

    private Optional<MutablePoint> firstPointWithAltitude(SortedSet<MutablePoint> points) {
        return points.stream().filter(hasNullAltitude.negate()).findFirst();
    }

    private Optional<MutablePoint> firstPointWithoutAltitude(SortedSet<MutablePoint> points) {
        return points.stream().filter(hasNullAltitude).findFirst();
    }

    private void extrapolateAltitudes(SortedSet<MutablePoint> missingAltitudePoints, MutablePoint referencePoint) {

        Distance referenceAltitude = referencePoint.altitude();

        for (MutablePoint point : missingAltitudePoints) {
            point.set(ALTITUDE, referenceAltitude);
        }
    }

    private void interpolateAltitudes(SortedSet<MutablePoint> missingAltitudePoints, MutablePoint startPoint, MutablePoint endPoint) {

        for (MutablePoint point : missingAltitudePoints) {

            Distance altitude = interpolate(
                startPoint.altitude(),
                endPoint.altitude(),
                timeFraction(startPoint.time(), endPoint.time(), point.time())
            );
            point.set(ALTITUDE, altitude);
        }
    }

    private double timeFraction(Instant startTime, Instant endTime, Instant testTime) {
        checkArgument(!startTime.equals(endTime));

        return (double) (testTime.toEpochMilli() - startTime.toEpochMilli()) / (endTime.toEpochMilli() - startTime.toEpochMilli());
    }
}
