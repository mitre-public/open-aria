package org.mitre.openaria.smoothing;

import static java.util.stream.Collectors.toCollection;

import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

public class ZeroAltitudeToNull<T> implements DataCleaner<Track<T>> {
    @Override
    public Optional<Track<T>> clean(Track<T> track) {

        TreeSet<Point<T>> cleanedPoints = track.points()
            .stream()
            .map(p -> pointWithNullAltitude(p))
            .collect(toCollection(TreeSet::new));

        return Optional.of(Track.of(cleanedPoints));
    }

    private Point<T> pointWithNullAltitude(Point<T> p) {

        return pointHasNegativeAltitude(p)
            ? Point.builder(p).altitude(null).build()
            : p;
    }

    private boolean pointHasNegativeAltitude(Point<T> p) {
        return !p.altitudeIsMissing() && p.altitude().inFeet() <= 0.0;
    }
}
