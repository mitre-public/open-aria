package org.mitre.openaria.airborne.exe;

import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.io.File;
import java.util.function.Consumer;

import org.mitre.openaria.airborne.AirborneAlgorithmDef;
import org.mitre.openaria.airborne.AirborneAria;
import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.core.TrackPair;

/**
 * This Demo show the simplest code for processing "pre-paired" location data.
 * <p>
 * This is intended to provide an easier on-ramp for getting to know the OpenARIA codebase.
 */
public class DemoProcessingTrackPair {


    public static void main(String[] args) {

        // --- Setup ---
        // This does the analysis work. It converts TrackPairs to AirborneEvents
        AirborneAria analyzer = airborneAria(algorithmDef());

        // This publishes output events to the durable storage of your choice
        //   For simplicity, we'll just write JSON to System.out
        Consumer<AirborneEvent> eventPublisher = (AirborneEvent event) -> {
            String eventAsJason = event.asJson();
            System.out.println(eventAsJason);
        };

        // Combine the "analyzer" and "eventPublisher" into a pipeline that processes TrackPairs
        AirbornePairConsumer analysisPipeline = new AirbornePairConsumer(
            analyzer, eventPublisher);

        // --- Get Location Data (For ONLY two aircraft) ---
        TrackPair dataFromTwoAircraft = makeTrackPairFromNopData(
            new File("open-aria-airborne/src/main/resources/scaryTrackData.txt"));

        // --- Process One TrackPair ---
        analysisPipeline.accept(dataFromTwoAircraft);
    }


    private static AirborneAlgorithmDef algorithmDef() {
        return AirborneAlgorithmDef.defaultBuilder()
            .verbose(true)
            .maxReportableScore(10000.0)
            .filterByAirspace(false)
            .publishAirborneDynamics(false)
            .build();
    }
}