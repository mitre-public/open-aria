
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
public class FillMissingAltitudes implements DataCleaner<Track> {

    private final HasNullAltitude hasNullAltitude = new HasNullAltitude();

    @Override
    public Optional<Track> clean(Track track) {

        TreeSet<Point> points = new TreeSet<>(track.points());

        Optional<Point> firstNonNull = firstPointWithAltitude(points);
        if (!firstNonNull.isPresent()) {
            return Optional.empty();
        }

        SortedSet<Point> pointsMissingAltitude = points.headSet(firstNonNull.get());
        TreeSet<Point> fixedPoints = extrapolateAltitudes(pointsMissingAltitude, firstNonNull.get());
        pointsMissingAltitude.clear();
        points.addAll(fixedPoints);


        Optional<Point> gapStart;
        Optional<Point> gapEnd = firstNonNull;

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

    private Optional<Point> firstPointWithAltitude(SortedSet<Point> points) {
        return points.stream().filter(hasNullAltitude.negate()).findFirst();
    }

    private Optional<Point> firstPointWithoutAltitude(SortedSet<Point> points) {
        return points.stream().filter(hasNullAltitude).findFirst();
    }

    /** For each point that is missing an altitude create a "patched point. */
    private TreeSet<Point> extrapolateAltitudes(SortedSet<Point> missingAltitudePoints, Point referencePoint) {

        Distance referenceAltitude = referencePoint.altitude();

        TreeSet<Point> fixedPoints = missingAltitudePoints.stream()
            .map(prior -> Point.builder(prior).butAltitude(referenceAltitude).build())
            .collect(toCollection(TreeSet::new));

        return fixedPoints;
    }

    private TreeSet<Point> interpolateAltitudes(SortedSet<Point> missingAltitudePoints, Point startPoint, Point endPoint) {

        TreeSet<Point> fixedPoints = missingAltitudePoints.stream()
            .map(pt -> {
                Distance altitude = interpolate(
                    startPoint.altitude(),
                    endPoint.altitude(),
                    timeFraction(startPoint.time(), endPoint.time(), pt.time())
                );
                return Point.builder(pt).butAltitude(altitude).build();
            }).collect(toCollection(TreeSet::new));

        return fixedPoints;

//        for (Point point : missingAltitudePoints) {
//
//            Distance altitude = interpolate(
//                startPoint.altitude(),
//                endPoint.altitude(),
//                timeFraction(startPoint.time(), endPoint.time(), point.time())
//            );
//            point.set(ALTITUDE, altitude);
//        }
    }

    private double timeFraction(Instant startTime, Instant endTime, Instant testTime) {
        checkArgument(!startTime.equals(endTime));

        return (double) (testTime.toEpochMilli() - startTime.toEpochMilli()) / (endTime.toEpochMilli() - startTime.toEpochMilli());
    }
}
