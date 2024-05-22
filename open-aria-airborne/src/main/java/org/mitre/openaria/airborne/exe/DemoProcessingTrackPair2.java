package org.mitre.openaria.airborne.exe;

import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.extractTracks;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.mitre.openaria.airborne.AirborneAlgorithmDef;
import org.mitre.openaria.airborne.AirborneAria;
import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvHit;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvParser;


/**
 * This Demo show the simplest code for processing "pre-paired" location data (from raw data that
 * isn't NOP data).
 * <p>
 * This is intended to provide an easier on-ramp for getting to know the OpenARIA codebase.
 */
public class DemoProcessingTrackPair2 {


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
        File fileOfCsvData = new File("open-aria-airborne/src/main/resources/scaryTrackData_openAriaCsv.txt");
        AriaCsvParser parser = new AriaCsvParser(fileOfCsvData);
        // The raw file is not sorted by time (the data is grouped by flight, we have to sort it to use a TrackMaker)
        List<Point<AriaCsvHit>> points = parser.stream().sorted().toList();
        List<Track<AriaCsvHit>> tracks = extractTracks(points.iterator());
        TrackPair dataFromTwoAircraft = TrackPair.from(tracks);

        // --- Process One TrackPair ---
        analysisPipeline.accept(dataFromTwoAircraft);
    }


    private static AirborneAlgorithmDef algorithmDef() {
        return AirborneAlgorithmDef.defaultBuilder()
            .verbose(true)
            .requireDataTag(false)
            .maxReportableScore(10000.0)
            .filterByAirspace(false)
            .publishAirborneDynamics(false)
            .build();
    }
}