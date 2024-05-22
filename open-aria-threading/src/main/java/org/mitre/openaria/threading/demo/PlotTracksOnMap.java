package org.mitre.openaria.threading.demo;

import static org.mitre.caasd.commons.ConsumingCollections.ConsumingArrayList;
import static org.mitre.openaria.threading.TrackMaking.makeTracksFromNopData;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.maps.MapBuilder;
import org.mitre.caasd.commons.maps.MapFeature;
import org.mitre.caasd.commons.maps.MapFeatures;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.nop.NopHit;

import com.google.common.io.Files;


/**
 * This program takes as input "one file of NOP data" and generates a map of all the tracks in that
 * file. The map is saved to the working directory as a File named "map-of-INPUT_FILE_NAME.png"
 */
public class PlotTracksOnMap {

    static String DEFAULT_INPUT_FILE = "/Users/jiparker/rawData/data-from-2021-05-30/STARS_ROA_RH_20210530.txt.gz";

    static int MAP_WIDTH_IN_PIXELS = 1280;
    static int MAP_ZOOM_LEVEL = 7;
    static Color TRACK_COLOR = new Color(1.0f, 1.0f, 1.0f, .1f);
    static float TRACK_STROKE_WIDTH = 2.0f;


    public static void main(String[] args) {

        boolean usingCommandLineArg = args.length == 1;

        String messageToUser = (usingCommandLineArg)
            ? "Attempting to Parse and Render: " + args[0]
            : "No input file given on command line, processing:" + DEFAULT_INPUT_FILE;

        System.out.println(messageToUser);

        String inputFileName = (usingCommandLineArg)
            ? args[0]
            : DEFAULT_INPUT_FILE;

        runDemo(new File(inputFileName));
    }

    private static void runDemo(File dataFile) {

        //hopefully collecting all data into memory doesn't blow-out memory!
        ConsumingArrayList<Track<NopHit>> tracks = makeTracksFromNopData(dataFile);

        plotMap(tracks, Files.getNameWithoutExtension(dataFile.getName()));
    }

    /**
     * Create a map of some input Tracks
     *
     * @param tracks           A list of Tracks
     * @param outputFilePrefix Piece for output filename (e.g. "ROA" --> "map-of-ROA.png")
     */
    public static <T> void plotMap(ArrayList<Track<T>> tracks, String outputFilePrefix) {

        MapBuilder.newMapBuilder()
            .mapBoxDarkMode()
//            .solidBackground(Color.BLACK)  //use this if you don't have an API token
            .center(computeAverageLatLong(tracks))
            .width(MAP_WIDTH_IN_PIXELS, MAP_ZOOM_LEVEL)
            .addFeatures(tracks, track -> asMapFeature(track))
            .toFile(new File("map-of-" + outputFilePrefix + ".png"));
    }

    public static <T> LatLong computeAverageLatLong(ArrayList<Track<T>> tracks) {

        List<LatLong> locations = tracks.stream()
            .flatMap(track -> track.points().stream())
            .map(point -> ((Point) point).latLong())
            .toList();

        return LatLong.avgLatLong(locations.toArray(new LatLong[0]));
    }


    public static MapFeature asMapFeature(Track track) {

        List<LatLong> locations = (List<LatLong>) track.points().stream()
            .map(obj -> ((Point) obj).latLong())
            .collect(Collectors.toList());

        return MapFeatures.path(locations, TRACK_COLOR, TRACK_STROKE_WIDTH);
    }

}
