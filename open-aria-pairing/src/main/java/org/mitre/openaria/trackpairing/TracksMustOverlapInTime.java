
package org.mitre.openaria.trackpairing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.mitre.caasd.commons.DataFilter;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.openaria.core.TrackPair;

/**
 * This DataFilter removes TrackPair when the they do not overlap in time. This can occur when track
 * smoothing removes points in a Track (usually due to removing low-speed or coasted points).
 */
class TracksMustOverlapInTime extends DataFilter<TrackPair> {

    TracksMustOverlapInTime(Duration minOverlap) {
        this(NO_OP_CONSUMER, minOverlap);
    }

    TracksMustOverlapInTime(Consumer<TrackPair> onRemoval, Duration minOverlap) {
        super(makePredicate(minOverlap), onRemoval);
    }

    private static Predicate<TrackPair> makePredicate(Duration minOverlap) {
        checkNotNull(minOverlap);
        checkArgument(!minOverlap.isNegative());
        return trackPair
            -> trackPair.overlapInTime()
            && ((TimeWindow) trackPair.timeOverlap().get()).duration().toMillis() > minOverlap.toMillis();
    }
}
