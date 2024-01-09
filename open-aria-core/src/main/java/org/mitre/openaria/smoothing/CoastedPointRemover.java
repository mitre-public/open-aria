
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Sets.newTreeSet;

import java.util.Collection;
import java.util.Optional;

import org.mitre.openaria.core.NopPoints.CenterPoint;
import org.mitre.openaria.core.NopPoints.StarsPoint;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.SimpleTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;

public class CoastedPointRemover implements DataCleaner<Track> {

    @Override
    public Optional<Track> clean(Track track) {

        Collection<Point> points = newTreeSet(track.points());

        points.removeIf(p -> isCoasted(p));

        return (points.isEmpty())
            ? Optional.empty()
            : Optional.of(new SimpleTrack(points));
    }

    public static boolean isCoasted(Point p) {

        if (p instanceof CenterPoint) {
            return CenterSmoothing.isCoastedPoint((CenterPoint) p);
        } else if (p instanceof StarsPoint) {
            return StarsSmoothing.isCoastedPoint((StarsPoint) p);
        } else {
            return false;
        }
    }
}
