
package org.mitre.openaria.threading.demo;

import java.util.function.Consumer;

import org.mitre.openaria.core.Track;
import org.mitre.openaria.threading.FacilitySpecificTrackMaker;
import org.mitre.caasd.commons.CountingConsumer;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class TrackMakingDemo {

    //This is the data sample FAA provided
//	static String DEFAULT_DIRECTORY_OF_MANY_NOP_FILES = "/Users/jiparker/Documents/NOP";
//	static String DEFAULT_FACILITY_PREFIX = "A80";

    //This is an entire day's worth of data downloaded from: ml-gw01:/dmc3/asias/nop/
    static String DEFAULT_DIRECTORY_OF_MANY_NOP_FILES = "/Users/jiparker/rawData/2016-10-18";
    static String DEFAULT_FACILITY_PREFIX = "A80"; //A90 //ZMP

    public static void main(String[] args) {

        if (args.length == 2) {
            runDemo(
                args[0],
                Facility.toFacility(args[1])
            );
        } else {
            System.out.println(
                "Did not find 2 input paramters.  Running with default parameters: "
                    + DEFAULT_DIRECTORY_OF_MANY_NOP_FILES
                    + " and " + DEFAULT_FACILITY_PREFIX);

            runDemo(
                DEFAULT_DIRECTORY_OF_MANY_NOP_FILES,
                Facility.toFacility(DEFAULT_FACILITY_PREFIX)
            );
        }
    }

    private static void runDemo(String directoryOfData, Facility facility) {

        CountingConsumer<Track> outputMechaism = outputMechaism(false);

        FacilitySpecificTrackMaker demo = new FacilitySpecificTrackMaker(
            directoryOfData,
            outputMechaism
        );

        demo.makeTracksFor(facility);

        System.out.println(demo.numPointsProcessed() + " points were processed");
        System.out.println(outputMechaism.numCallsToAccept() + " tracks were process");
    }

    /**
     * Create a "no-op" CommonTrack Consumer that counts how many CommonTracks it didn't operate
     * on.
     *
     * @param printAsYouGo Set this flag to true if you want a message written to System.out
     *                     whenever a track is provided.
     *
     * @return
     */
    private static CountingConsumer<Track> outputMechaism(boolean printAsYouGo) {

        Consumer<Track> consumer;

        if (printAsYouGo) {
            consumer = (Track t) -> {
                System.out.println("Found Track: " + t.callsign() + " " + t.size());
            };
        } else {
            consumer = (Track t) -> {};
        }

        return new CountingConsumer<>(consumer);
    }
}
