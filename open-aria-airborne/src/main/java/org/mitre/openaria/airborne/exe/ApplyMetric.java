package org.mitre.openaria.airborne.exe;

import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;

import java.util.function.Consumer;

import org.mitre.openaria.airborne.AirborneAlgorithmDef;
import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.airborne.AirbornePairConsumer;

/**
 * This program applies a Risk Metric algorithm to a directory's worth of TrackPair files (text
 * files that contain the raw NOP data from exactly two aircraft).
 *
 * <p>The goal of this program is to provide an easy way to manually evaluating arbitrary
 * TrackPairs with a RiskMetricTrackPairConsumer that has been configured as necessary.
 */
public class ApplyMetric {

    //	static String DIRECTORY_OF_DATA = "/Users/jiparker/Desktop/importantEvents/scaryEvents";
    static String DIRECTORY_OF_DATA = "/Users/jiparker/Desktop/importantEvents/bryanRequests";
    //static String DIRECTORY_OF_DATA = "/Users/jiparker/Desktop/importantEvents/posterChildEvents";
    //static String DIRECTORY_OF_DATA = "/Users/jiparker/Desktop/importantEvents/levelFlightExamples";
    //static String DIRECTORY_OF_DATA = "/Users/jiparker/Desktop/importantEvents/exampleForFormulaChange";
    //static String DIRECTORY_OF_DATA = "/Users/jiparker/Desktop/importantEvents/parallelApproach";

    public static void main(String[] args) {

        Consumer<AirborneEvent> printScore
            = (event) -> System.out.println("FINAL SCORE: " + event.score());

        Consumer<AirborneEvent> printRecord
            = (event) -> System.out.println(event.asJson());

        AirbornePairConsumer airborneAlgorithm = new AirbornePairConsumer(
            airborneAria(airborneProperties()),
            printScore.andThen(printRecord)
        );

        Experiment experiment = new Experiment(
            DIRECTORY_OF_DATA,
            airborneAlgorithm
        );

        experiment.runExperiment();
    }

    static AirborneAlgorithmDef airborneProperties() {
        return defaultBuilder()
            .verbose(true)
            .maxReportableScore(10000.0)
            .filterByAirspace(false)
            .publishAirborneDynamics(false)
            .build();
    }
}
