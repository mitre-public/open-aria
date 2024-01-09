
package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.DataCleaner;

public class AirborneAriaTest {

    TrackPair scaryTrackPair() {
        return makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"));
    }

    TrackPair scaryTrackPairWithoutCallsigns() {
        return makeTrackPairFromNopData(getResourceFile("scaryTrackData_noCallSign.txt"));
    }

    TrackPair safeTrackPair() {
        return makeTrackPairFromNopData(getResourceFile("safeTrackData.txt"));
    }


    @Test
    public void scaryDataProducesEvent() {

        AirborneAria aa = airborneAria();

        ArrayList<AirborneEvent> eventList = aa.findAirborneEvents(scaryTrackPair());

        assertThat(eventList, hasSize(1));
    }

    @Test
    public void safeDataProducesNoEvent() {

        AirborneAria aa = airborneAria();

        ArrayList<AirborneEvent> eventList = aa.findAirborneEvents(safeTrackPair());

        assertThat(eventList, empty());
    }

    @Test
    public void scaryDataWithoutCallsignsIsScreenedOut() {

        AirborneAria aa = airborneAria();

        ArrayList<AirborneEvent> eventList = aa.findAirborneEvents(scaryTrackPairWithoutCallsigns());

        assertThat(eventList, empty());
    }

    @Test
    public void verifyEventWithoutCallsignsCanBeRetainedIfScreeningIsTurnedOff() {

        //override the default behavior
        AirborneAlgorithmDef def = defaultBuilder()
            .requireDataTag(false)
            .build();

        AirborneAria aa = airborneAria(def);

        ArrayList<AirborneEvent> eventList = aa.findAirborneEvents(scaryTrackPairWithoutCallsigns());

        assertThat(eventList, hasSize(1));
    }

    /*
     * The goal of this test is to verify that AirborneAria does not fail when the smoother returns
     * an empty optional. Therefore, we override the AirborneProperties object so that when it
     * provides the Track cleaner it provides a Track cleaner that ALWAYS returns an empty result.
     */
    @Test
    public void survivesBadInput() {

        //The AirborneProperties provides a pairCleaner that suppresses all data
        class InputSuppressingAlgorithmDef extends AirborneAlgorithmDef {

            InputSuppressingAlgorithmDef() {
                super();
            }

            @Override
            public DataCleaner<TrackPair> pairCleaner() {
                return DataCleaner.suppressAll();
            }
        }

        AirborneAria aa = airborneAria(new InputSuppressingAlgorithmDef());

        ArrayList<AirborneEvent> eventList = aa.findAirborneEvents(scaryTrackPair());

        assertThat(eventList, empty());
    }
}
