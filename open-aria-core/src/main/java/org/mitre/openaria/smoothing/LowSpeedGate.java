
package org.mitre.openaria.smoothing;

import static org.mitre.openaria.core.PointField.SPEED;

import java.util.Optional;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.caasd.commons.DataCleaner;

/**
 * A LowSpeedGate changes speeds which are below the low speed threshold to 0.
 */
public class LowSpeedGate implements DataCleaner<MutableTrack> {

    private static final double LOW_SPEED_THRESHOLD = 5.0;

    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        for (MutablePoint point : track.points()) {
            if (point.speedInKnots() < LOW_SPEED_THRESHOLD) {
                point.set(SPEED, 0.0);
            }
        }
        return Optional.of(track);
    }
}
