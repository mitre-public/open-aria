
package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;
import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.io.File;
import java.io.FileReader;

import org.mitre.caasd.commons.ConsumingCollections;
import org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;
import org.mitre.openaria.airborne.metrics.EventSummarizer;
import org.mitre.openaria.core.TrackPair;

import org.junit.jupiter.api.Test;

public class AirbornePairConsumerTest {

    @Test
    public void qualifyingEventsAreTracked() {

        AirbornePairConsumer consumer = new AirbornePairConsumer(
            airborneAria(),
            NO_OP_CONSUMER
        );

        EventSummarizer stats = consumer.getEventSummarizer();
        assertThat(stats.eventCount(), is(0));

        TrackPair testTrack = makeTrackPairFromNopData(new File("src/test/resources/scaryTrackData.txt"));

        consumer.accept(testTrack);

        assertThat(stats.eventCount(), is(1));
    }

    @Test
    public void nonQualifyingEventsAreNotTracked() {

        AirborneAlgorithmDef def = defaultBuilder()
            .maxReportableScore(0.001)
            .build();

        //no TrackPairs should generate an event because the score is alway 5 and the ceiling is 2
        AirbornePairConsumer consumer = new AirbornePairConsumer(
            airborneAria(def),
            NO_OP_CONSUMER
        );

        EventSummarizer stats = consumer.getEventSummarizer();
        assertThat(stats.eventCount(), is(0));

        TrackPair testTrack = makeTrackPairFromNopData(new File("src/test/resources/scaryTrackData.txt"));
//        TrackPair testTrack = makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"));

        //ingesting this scary track did nothing because we set the "maxReportableScore" to such an absurd value
        consumer.accept(testTrack);

        assertThat(stats.eventCount(), is(0));
    }

    TrackPair scaryTrackPair() {

        File f = new File("src/test/resources/scaryTrackData.txt");

//        return makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"));
        return makeTrackPairFromNopData(f);
    }


    @Test
    public void endToEndProcessing() {

        AirborneAria tpp = airborneAria(new AirborneAlgorithmDef());

        //catch AirborneEvent generated when the airborne algorithm processes data
        ConsumingCollections.ConsumingArrayList<AirborneEvent> eventCatcher = newConsumingArrayList();

        //Create the actual TrackPair processing mechanism
        AirbornePairConsumer airborneAria = new AirbornePairConsumer(
            tpp,
            eventCatcher
        );

        //analyze this track
        airborneAria.accept(scaryTrackPair());

        //confirm exactly one event was found
        assertThat(eventCatcher, hasSize(1));

        AirborneEvent result = eventCatcher.get(0);

        //this is a scary event, it should have a low score
        assertThat(result.score(), lessThan(5.0));
    }

    /*
     * This test WILL break when something changes AirborneEvent data (content and non-white space
     * formating). If the test breaks, you'll need to manually update the "expectedUuid" variable
     * below and our "official record" of the output the last time that algorithm was run
     *
     * To do this, (1) uncomment that "System.out" statement, (2) overwrite "scaryTrackOutput.json"
     * with the new "correct record format", and (3) copy the new uuid into this unit test.
     */
    @Test
    public void outputFormattingHasNotChanged() throws Exception {

        //reprocess some known data
        AirborneEvent eventFromComputation = processCannedPair(scaryTrackPair());
        String json = eventFromComputation.asJson();

		System.out.println("Event From Computation:\n" + json);

        //ALSO, load the output from the last time this canned data was processed
        //this file was created by saving the result calling "String json = eventFromComputation.asJson()"
        File file = new File("src/test/resources/scaryTrackOutput.json");
        AirborneEvent eventFromFile = AirborneEvent.parse(new FileReader(file));

        assertThat(eventFromComputation.asJson(), is(eventFromFile.asJson()));

        String expectedUuid = "60b539d0929a32821a182f3e369884c5";
        assertThat(eventFromComputation.uuid(), is(expectedUuid));
        assertThat(json.contains(expectedUuid), is(true));

        String expectedFormat = "3"; //AirborneEvent.SCHEMA_VERSION;
        assertThat(eventFromComputation.schemaVersion(), is(expectedFormat));
    }

    private AirborneEvent processCannedPair(TrackPair pair) {

        //catch AirborneEvent generated when the airborne algorithm processes data
        ConsumingArrayList<AirborneEvent> eventCatcher = newConsumingArrayList();

        AirborneAlgorithmDef def = defaultBuilder()
            .publishAirborneDynamics(true)
            .build();

        //Create the actual TrackPair processing mechanism
        AirbornePairConsumer airborneAria = new AirbornePairConsumer(
            airborneAria(def),
            eventCatcher
        );

        //analyze this track
        airborneAria.accept(pair);

        AirborneEvent result = eventCatcher.get(0);
        return result;
    }
}
