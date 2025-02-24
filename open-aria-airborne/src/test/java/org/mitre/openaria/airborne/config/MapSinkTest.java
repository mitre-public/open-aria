package org.mitre.openaria.airborne.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.mitre.openaria.airborne.AirborneAria;
import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.nop.NopHit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class MapSinkTest {

    TrackPair<NopHit> scaryTrackPair() {
        File file = new File("src/test/resources/scaryTrackData.txt");
        return makeTrackPairFromNopData(file);
    }

    @Test
    void makeMapsFromEvents_plainMaps() throws IOException {

        String TARGET_DIR = "eventMaps";
        File mapDirectory = new File(TARGET_DIR);

        // The directory the MapSink writes Maps to is empty...
        assertThat(mapDirectory.exists(), is(false));

        MapSink mapSink = new MapSink();
        mapSink.accept(testAirborneEvent());

        assertThat(mapDirectory.exists(), is(true));
        File[] mapFiles = mapDirectory.listFiles();
        assertThat(mapFiles.length, is(1));

        cleanUp(mapDirectory);
    }


    @Disabled // because not everyone will have MapBox
    @Test
    void makeMapsFromEvents_withMapBox() throws IOException {

        String TARGET_DIR = "mapDir";
        File mapDirectory = new File(TARGET_DIR);

        // The directory the MapSink writes Maps to is empty...
        assertThat(mapDirectory.exists(), is(false));

        MapSink mapSink = new MapSink(TARGET_DIR, true, 20);
        mapSink.accept(testAirborneEvent());

        assertThat(mapDirectory.exists(), is(true));
        File[] mapFiles = mapDirectory.listFiles();
        assertThat(mapFiles.length, is(1));

        cleanUp(mapDirectory);
        cleanUp(new File("mapTileCache"));
    }


    void cleanUp(File dirOfFiles) throws IOException {

        assertThat(dirOfFiles.exists(), is(true));
        File[] contents = dirOfFiles.listFiles();

        Stream.of(contents).forEach(file -> file.delete());
        Files.deleteIfExists(dirOfFiles.toPath());
    }

    /** Generate an example AirborneEvent by running a "canned" example */
    AirborneEvent testAirborneEvent() {

        AirborneAria aa = airborneAria();
        ArrayList<AirborneEvent> eventList = aa.findAirborneEvents(scaryTrackPair());
        assertThat(eventList, hasSize(1));

        return eventList.get(0);
    }

}