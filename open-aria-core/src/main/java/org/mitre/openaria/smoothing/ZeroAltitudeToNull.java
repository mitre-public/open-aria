package org.mitre.openaria.smoothing;

import static java.util.stream.Collectors.toCollection;

import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;

public class ZeroAltitudeToNull implements DataCleaner<MutableTrack> {
    @Override
    public Optional<MutableTrack> clean(MutableTrack mutableTrack) {

        TreeSet<Point> cleanedPoints = mutableTrack.points().stream()
            .map(p -> pointWithNullAltitude(p))
            .collect(toCollection(TreeSet::new));

        return Optional.of(MutableTrack.of(cleanedPoints));
    }

    private Point pointWithNullAltitude(Point p) {

        return pointHasNegativeAltitude(p)
            ? Point.builder(p).butAltitude(null).build()
            : p;
    }

    private boolean pointHasNegativeAltitude(Point p) {
        return !p.altitudeIsMissing() && p.altitude().inFeet() <= 0.0;
    }
}
