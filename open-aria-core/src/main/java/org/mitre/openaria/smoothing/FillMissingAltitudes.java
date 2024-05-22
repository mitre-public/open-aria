
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toCollection;
import static org.mitre.openaria.core.Interpolate.interpolate;

import java.time.Instant;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

/**
 * This DataCleaner adds altitude data to points in a MutableTrack that do not have altitude data.
 * <p>
 * If the first points of a track are missing altitudes, then they are filled with the first present
 * altitude. If the last points of a track are missing altitudes, then they are filled with the last
 * present altitude. If intermediate points of a track are missing altitudes, then they are
 * interpolated between the previous point and the following point. If all the points in a track are
 * missing altitudes, then the track is removed.
 */
public class FillMissingAltitudes<T> implements DataCleaner<Track<T>> {

    private final HasNullAltitude hasNullAltitude = new HasNullAltitude();

    @Override
    public Optional<Track<T>> clean(Track<T> track) {

        TreeSet<Point<T>> points = new TreeSet<>(track.points());

        Optional<Point<T>> firstNonNull = firstPointWithAltitude(points);
        if (!firstNonNull.isPresent()) {
            return Optional.empty();
        }

        SortedSet<Point<T>> pointsMissingAltitude = points.headSet(firstNonNull.get());
        TreeSet<Point<T>> fixedPoints = extrapolateAltitudes(pointsMissingAltitude, firstNonNull.get());
        pointsMissingAltitude.clear();
        points.addAll(fixedPoints);


        Optional<Point<T>> gapStart;
        Optional<Point<T>> gapEnd = firstNonNull;

        while (gapEnd.isPresent()) {

            gapStart = firstPointWithoutAltitude(points.tailSet(gapEnd.get()));

            if (!gapStart.isPresent()) {
                break;
            }
            gapEnd = firstPointWithAltitude(points.tailSet(gapStart.get()));

            if (!gapEnd.isPresent()) {

                pointsMissingAltitude = points.tailSet(gapStart.get());
                fixedPoints = extrapolateAltitudes(pointsMissingAltitude, points.lower(gapStart.get()));
                pointsMissingAltitude.clear();
                points.addAll(fixedPoints);

//                extrapolateAltitudes(points.tailSet(gapStart.get()), points.lower(gapStart.get()));
            } else {
                pointsMissingAltitude = points.subSet(gapStart.get(), gapEnd.get());
                fixedPoints = interpolateAltitudes(pointsMissingAltitude, points.lower(gapStart.get()), gapEnd.get());
                pointsMissingAltitude.clear();
                points.addAll(fixedPoints);

//                interpolateAltitudes(points.subSet(gapStart.get(), gapEnd.get()), points.lower(gapStart.get()), gapEnd.get());
            }
        }

        return Optional.of(Track.of(points));
    }

    private Optional<Point<T>> firstPointWithAltitude(SortedSet<Point<T>> points) {
        return points.stream().filter(hasNullAltitude.negate()).findFirst();
    }

    private Optional<Point<T>> firstPointWithoutAltitude(SortedSet<Point<T>> points) {
        return points.stream().filter(hasNullAltitude).findFirst();
    }

    /** For each point that is missing an altitude create a "patched point". */
    private TreeSet<Point<T>> extrapolateAltitudes(SortedSet<Point<T>> missingAltitudePoints, Point<T> referencePoint) {

        Distance referenceAltitude = referencePoint.altitude();

        TreeSet<Point<T>> fixedPoints = missingAltitudePoints.stream()
            .map(prior -> Point.builder(prior).altitude(referenceAltitude).build())
            .collect(toCollection(TreeSet::new));

        return fixedPoints;
    }

    private TreeSet<Point<T>> interpolateAltitudes(SortedSet<Point<T>> missingAltitudePoints, Point<T> startPoint, Point<T> endPoint) {

        TreeSet<Point<T>> fixedPoints = missingAltitudePoints.stream()
            .map(pt -> {
                Distance altitude = interpolate(
                    startPoint.altitude(),
                    endPoint.altitude(),
                    timeFraction(startPoint.time(), endPoint.time(), pt.time())
                );
                return Point.builder(pt).altitude(altitude).build();
            }).collect(toCollection(TreeSet::new));

        return fixedPoints;
    }

    private double timeFraction(Instant startTime, Instant endTime, Instant testTime) {
        checkArgument(!startTime.equals(endTime));

        return (double) (testTime.toEpochMilli() - startTime.toEpochMilli()) / (endTime.toEpochMilli() - startTime.toEpochMilli());
    }
}
