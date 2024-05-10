
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Sets.newTreeSet;

import java.util.Optional;
import java.util.TreeSet;

import org.mitre.caasd.commons.DataCleaner;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.NopHit;
import org.mitre.openaria.core.formats.nop.CenterRadarHit;
import org.mitre.openaria.core.formats.nop.StarsRadarHit;

public class CoastedPointRemover implements DataCleaner<Track> {

    @Override
    public Optional<Track> clean(Track track) {

        TreeSet<Point> points = newTreeSet(track.points());

        points.removeIf(p -> isCoasted(p));

        return (points.isEmpty())
            ? Optional.empty()
            : Optional.of(Track.of((TreeSet) points));
    }

    public static boolean isCoasted(Point p) {

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
