
package org.mitre.openaria.threading;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.function.Consumer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class BulkTrackMakingTest {

    //This is an entire day's worth of data downloaded from: ml-gw01:/dmc3/asias/nop/
    static String DEFAULT_DIRECTORY_OF_MANY_NOP_FILES = "/Users/jiparker/rawData/2016-10-18";

    //how many tracks have been processed from a particular facility.
    static int numTracks = 0;

    @Disabled //this is an expensive unit test to run.  There is nothing wrong with it.
    @Test
    public void testBulkTrackMaking() {
        /*
         * This test ensures that reapplying the TrackMaking logic to the Points found in each
         * output Track produces exactly 1 Track
         */
        for (Facility facility : Facility.values()) {
            threadDataFromOneFacility(DEFAULT_DIRECTORY_OF_MANY_NOP_FILES, facility);
        }
    }

    private void threadDataFromOneFacility(String directoryOfData, Facility facility) {

        numTracks = 0;

        FacilitySpecificTrackMaker trackMaker = new FacilitySpecificTrackMaker(
            directoryOfData,
            trackTester()
        );

        trackMaker.makeTracksFor(facility);

        //show progress because this unit test takes a while to run
        System.out.println(facility.name() + " had " + numTracks + " tracks");
    }

    private Consumer<Track> trackTester() {
        return new Consumer<Track>() {
            @Override
            public void accept(Track t) {
                confirmExactlyOneTrack(t);
                numTracks++;
            }
        };
    }

    public static void confirmExactlyOneTrack(Track inputTrack) {
        /*
         * This method confirms that if you reapply the "Track making" logic to the points contained
         * in a single Track you always get back a single Track
         */

        Collection<Track> tracks = TrackMaking.extractTracks(
            inputTrack.points().iterator()
        );

        if (tracks.size() == 1) {
            //Repplying Track Making logic to the points in 1 Track should result in 1 track
            assertThat(tracks.size(), is(1));
        } else {
            printFailingTrack(tracks, inputTrack);
            fail("Failing on Track number: " + numTracks);
        }
    }

    private static void printFailingTrack(Collection<Track> tracks, Track inputTrack) {
        System.out.println(tracks.size() + " tracks found:");

        System.out.println("\nAll input Tracks");
        for (Point point : inputTrack.points()) {
            System.out.println(point.asNop());
        }

        int i = 0;
        for (Track track : tracks) {
            System.out.println("Track " + i);

            for (Point point : track.points()) {
                System.out.println(point.asNop());
            }

            i++;
        }
    }
}
