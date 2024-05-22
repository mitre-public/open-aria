
package org.mitre.openaria.smoothing;

import java.util.HashSet;

import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.StarsRadarHit;

import com.google.common.collect.Sets;

public class StarsSmoothing {

    /**
     * A CenterRadarHit will have one of these values in its cmsField153A field if that
     * CenterRadarHit is a coasted hit.
     */
    private static final HashSet<String> STARS_COASTED_FLAGS = Sets.newHashSet("CST", "DRP");

    /**
     * @param starsRh A StarsRadarHit
     *
     * @return True if CenterRadarHit is a "coasted" radar hit
     */
    public static boolean isCoastedRadarHit(StarsRadarHit starsRh) {

        String statusFieldValue = starsRh.trackStatus();

        return (statusFieldValue == null)
            ? false
            : STARS_COASTED_FLAGS.contains(statusFieldValue);
    }

    public static boolean isCoastedPoint(NopHit starsPoint) {
        StarsRadarHit srh = (StarsRadarHit) starsPoint.rawMessage();
        return isCoastedRadarHit(srh);
    }
}
