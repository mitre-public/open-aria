
package org.mitre.openaria.trackpairing;

import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.util.function.Consumer;

import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.DataFilter;

/**
 * This DataFilter removes TrackPairs that appear to be flying in formation. This determination is
 * made by tracking how long two Tracks stay within a fixed lateral distance (altitude is ignored).
 * If two flights stay with X Nautical miles for more than Y seconds then the tracks are filtered
 * out.
 */
class RemoveFormationFlights extends DataFilter<TrackPair> {

    RemoveFormationFlights(IsFormationFlight test) {
        this(test, NO_OP_CONSUMER);
    }

    RemoveFormationFlights(IsFormationFlight test, Consumer<TrackPair> onRemoval) {
        //THIS NEGATE IS IMPORTANT !! ALL CONSTRUCTORS SHOULD BE ROUTED THROUGH HERE
        super(test.negate(), onRemoval);
    }

}
