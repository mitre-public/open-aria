
package org.mitre.openaria.trackpairing;

import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.util.function.Consumer;

import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.DataFilter;

/**
 * This DataFilter removes TrackPairs when the aircraft NEVER separate by a required distance. The
 * purpose of this filter is to suppress events caused when a single aircraft is represented in two
 * tracks.
 */
class RequireSeparation extends DataFilter<TrackPair> {

    RequireSeparation(double requiredDivergenceInNauticalMiles) {
        this(requiredDivergenceInNauticalMiles, NO_OP_CONSUMER);
    }

    RequireSeparation(double requiredDivergenceInNauticalMiles, Consumer<TrackPair> onRemoval) {
        super(
            pair -> pair.separateBy(requiredDivergenceInNauticalMiles),
            onRemoval
        );
    }
}
