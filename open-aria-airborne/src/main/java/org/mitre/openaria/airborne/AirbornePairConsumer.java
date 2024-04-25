package org.mitre.openaria.airborne;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.function.Consumer;

import org.mitre.openaria.airborne.metrics.EventSummarizer;
import org.mitre.openaria.core.TrackPair;

/**
 * A AirbornePairConsumer analyzes incoming TrackPairs and forwards all detected events to a
 * downstream {@literal Consumer<AirborneEvent>}.
 *
 * <p>A AirbornePairConsumer also provides access to any statistics gathered by the
 * TrackPairProcessor
 * provided at construction.
 */
public class AirbornePairConsumer implements Consumer<TrackPair> {

    /** Combines the DataCleaning and EventDetection into a single operation */
    private final AirborneAria airborneAlgorithm;

    /**
     * Any events the AirborneAria algorithm detects are sent here. The OutputDestination is likely
     * to perform a task like writing to the local file system, writing to a remote database, or
     * sending messages to a Kafka Cluster.
     */
    private final Consumer<AirborneEvent> outputMechanism;

    private final EventSummarizer statCollector;

    /**
     * Create a Consumer that processes each incoming TrackPair. AirborneEvents detected when
     * processing a TrackPair are forwarded to the outputMechanism.
     *
     * @param airborneAria    This TrackPairProcessor detects RiskMetricEvents in input TrackPairs
     * @param outputMechanism The way this Consumer receives interesting events.
     */
    public AirbornePairConsumer(AirborneAria airborneAria, Consumer<AirborneEvent> outputMechanism) {
        this.airborneAlgorithm = checkNotNull(airborneAria);
        this.outputMechanism = checkNotNull(outputMechanism);
        this.statCollector = new EventSummarizer();
    }

    @Override
    public void accept(TrackPair trackPair) {
        checkNotNull(trackPair);

        ArrayList<AirborneEvent> detectedEvents = airborneAlgorithm.findAirborneEvents(trackPair);

        detectedEvents.forEach(statCollector);

        for (AirborneEvent detectedEvent : detectedEvents) {
            outputMechanism.accept(detectedEvent);
        }
    }

    public EventSummarizer getEventSummarizer() {
        return statCollector;
    }
}
