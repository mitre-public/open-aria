
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Sets.newTreeSet;

import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.nop.CenterRadarHit;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.StarsRadarHit;

public class CoastedPointRemover<T> implements DataCleaner<Track<T>> {

    @Override
    public Optional<Track<T>> clean(Track<T> track) {

        TreeSet<Point<T>> points = newTreeSet(track.points());

        points.removeIf(p -> isCoasted(p));

        return (points.isEmpty())
            ? Optional.empty()
            : Optional.of(Track.of(points));
    }

    public static <T> boolean isCoasted(Point<T> p) {

        if (p.rawData() instanceof NopHit nop) {
            if (nop.rawMessage() instanceof CenterRadarHit crh) {
                return CenterSmoothing.isCoastedRadarHit(crh);
            }
            if (nop.rawMessage() instanceof StarsRadarHit srh) {
                return StarsSmoothing.isCoastedRadarHit(srh);
            }
        }

        return false;
    }
}
