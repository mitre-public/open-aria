
package org.mitre.openaria.smoothing;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.nonNull;

import java.util.HashSet;

import org.mitre.openaria.core.formats.nop.CenterRadarHit;
import org.mitre.openaria.core.formats.nop.NopHit;

public class CenterSmoothing {

    /**
     * A CenterRadarHit will have one of these values in its cmsField153A field if that
     * CenterRadarHit is a coasted radar hit.
     */
    private static final HashSet<String> CENTER_COASTED_FLAGS = newHashSet("C");

    /**
     * @param centerRh A CenterRadarHit
     *
     * @return True if CenterRadarHit is a "coasted" radar hit
     */
    public static boolean isCoastedRadarHit(CenterRadarHit centerRh) {

        String cmsFieldValue = centerRh.cmsField153A();

        return nonNull(cmsFieldValue)
            ? CENTER_COASTED_FLAGS.contains(cmsFieldValue)
            : false;
    }

    public static boolean isCoastedPoint(NopHit centerPoint) {
        CenterRadarHit crh = (CenterRadarHit) centerPoint.rawMessage();
        return isCoastedRadarHit(crh);
    }
}
