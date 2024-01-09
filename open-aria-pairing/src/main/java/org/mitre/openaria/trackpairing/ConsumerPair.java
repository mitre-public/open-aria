
package org.mitre.openaria.trackpairing;

import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.util.function.Consumer;

import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;

/**
 * For efficiency reasons ARIA must create Tracks and TrackPairs in a single pass of the incoming
 * raw data. Consequently, ARIA must know what to do with Tracks and TrackPairs as they are made. A
 * ConsumerPair represents the "destination" of these Tracks and TrackPair.
 * <p>
 * This class was created because properly configuring these two downstream consumers is often a
 * significant piece of logic unto itself. This class helps separate the logic that configures the
 * downstream data handlers (i.e. these consumers) from the logic that feeds data to these data
 * handlers.
 */
public class ConsumerPair {

    private final Consumer<Track> trackHandler;

    private final Consumer<TrackPair> trackPairHandler;

    public ConsumerPair(Consumer<Track> singleConsumer, Consumer<TrackPair> pairConsumer) {
        this.trackHandler = (singleConsumer == null) ? NO_OP_CONSUMER : singleConsumer;
        this.trackPairHandler = (pairConsumer == null) ? NO_OP_CONSUMER : pairConsumer;
    }

    public static ConsumerPair of(Consumer<Track> trackConsumer, Consumer<TrackPair> trackPairConsumer) {
        return new ConsumerPair(trackConsumer, trackPairConsumer);
    }

    public Consumer<Track> trackConsumer() {
        return trackHandler;
    }

    public Consumer<TrackPair> pairConsumer() {
        return trackPairHandler;
    }
}
