

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.time.Duration;

import org.mitre.caasd.commons.Distance;

/**
 * This convenience class combines the time until the closest point of approach is reached with the
 * Distance between two aircraft at that moment in time.
 */
public class ClosestPointOfApproach implements Serializable {

    private final Duration duration;
    private final Distance distance;

    public ClosestPointOfApproach(Duration time, Distance dist) {
        this.duration = checkNotNull(time);
        this.distance = checkNotNull(dist);
    }

    public Duration timeUntilCpa() {
        return this.duration;
    }

    public Distance distanceAtCpa() {
        return this.distance;
    }
}
