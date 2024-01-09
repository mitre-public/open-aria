
package org.mitre.openaria.smoothing;

import java.util.function.Consumer;

import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataFilter;

/**
 * RemoveLowVariabilityTracks removes tracks that have a large number of Points that are distributed
 * across too few locations.
 * <p>
 * This filter is intended to removed faux-tracks generated when radars misinterpret a signal that
 * reflects off a stationary objects (like a radio tower or tall building). This flaw phenomenon
 * generates very long tracks that have very little movement.
 */
public class RemoveLowVariabilityTracks<T extends Track> extends DataFilter<T> {

    public RemoveLowVariabilityTracks() {
        this(new HasLowVariability<>(), ignored -> {});
    }

    public RemoveLowVariabilityTracks(HasLowVariability<T> test) {
        this(test, ignored -> {});
    }

    public RemoveLowVariabilityTracks(HasLowVariability<T> test, Consumer<T> onRemoval) {
        //THIS NEGATE IS IMPORTANT !! ALL CONSTRUCTORS SHOULD BE ROUTED THROUGH HERE
        super(test.negate(), onRemoval);
    }
}
