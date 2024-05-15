package org.mitre.openaria.smoothing;

import static java.util.stream.Collectors.toCollection;

import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;

public class ZeroAltitudeToNull implements DataCleaner<Track> {
    @Override
    public Optional<Track> clean(Track track) {

        TreeSet<Point> cleanedPoints = ((NavigableSet<Point<?>>) track.points())
            .stream()
            .map(p -> pointWithNullAltitude(p))
            .collect(toCollection(TreeSet::new));

        return Optional.of(Track.ofRaw(cleanedPoints));
    }

    private Point<?> pointWithNullAltitude(Point<?> p) {

        return pointHasNegativeAltitude(p)
            ? Point.builder(p).altitude(null).build()
            : p;
    }

    private boolean pointHasNegativeAltitude(Point p) {
        return !p.altitudeIsMissing() && p.altitude().inFeet() <= 0.0;
    }
}
