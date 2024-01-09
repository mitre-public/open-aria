

package org.mitre.openaria.airborne;

import static org.mitre.openaria.core.TrackPairs.atLeastOneTrackHasAircraftId;
import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.util.function.Consumer;

import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.DataFilter;

/** This DataFilter removes TrackPairs when both Tracks are missing an AircraftId. */
public class RequireAtLeastOneAircraftId extends DataFilter<TrackPair> {

    /** Create a DataFilter that removes TrackPairs when both Tracks are missing an AircraftId */
    public RequireAtLeastOneAircraftId() {
        this(NO_OP_CONSUMER);
    }

    /**
     * Create a DataFilter that removes TrackPairs when both Tracks are missing an AircraftId
     *
     * @param onRemoval A Consumer that will receive TrackPairs that get filtered out
     */
    public RequireAtLeastOneAircraftId(Consumer<TrackPair> onRemoval) {
        super(
            trackPair -> atLeastOneTrackHasAircraftId(trackPair),
            onRemoval
        );
    }
}
