package org.mitre.openaria.threading.demo;

import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import org.mitre.openaria.core.ApproximateTimeSorter;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.output.HashUtils;
import org.mitre.openaria.threading.TrackMaker;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.LatLongPath;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.ids.TimeId;
import org.mitre.caasd.commons.parsing.nop.NopParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This program parses one file of NOP data and make JSON "NopSegments" from it.
 * <p>
 * The goal of this program is to make "example data" for the Data Architecture project
 */
public class MakeNopSegments {

    static final String DEFAULT_FILE = "/users/jiparker/rawData/data-from-2021-05-30/STARS_D10_RH_20210530.txt.gz";

    //    static final Gson GSON_CONVERTER = new GsonBuilder().create();
    static final Gson GSON_CONVERTER = new GsonBuilder().setPrettyPrinting().create();  //great for human-readable examples, bad for parsing "files

    public static void main(String[] args) {

        File nopDataFile = (args.length == 1)
            ? new File(args[0])
            : new File(DEFAULT_FILE);

        makeNopSegmentsFrom(nopDataFile);
    }


    private static void makeNopSegmentsFrom(File fileOfNopData) {

        System.out.println("Parsing: " + fileOfNopData.getName());

        Consumer<Track> trackHandler = makeTrackHandler("D10-Segments.json");
        var trackMaker = new TrackMaker(trackHandler);
        var pointSorter = new ApproximateTimeSorter<>(Duration.ofMinutes(15), trackMaker);

        parseFileAndProcessData(fileOfNopData, pointSorter);
    }


    private static Consumer<Track> makeTrackHandler(String nameOfSinkFile) {

        final File targetFile = new File(nameOfSinkFile);

        Consumer<Track> consumer = (track) -> {
            try {
                FileUtils.appendToFile(targetFile, trackToJson(track));
            } catch (Exception e) {
                throw demote(e);
            }
        };

        return consumer;
    }

    private static String trackToJson(Track track) {
        NopSegment seg = new NopSegment(track);
        return GSON_CONVERTER.toJson(seg) + "\n";
    }

    private static void parseFileAndProcessData(File fileOfNopData, Consumer<Point> pointConsumer) {

        NopParser parser = new NopParser(fileOfNopData);
        PointIterator iter = new PointIterator(parser);

        while (iter.hasNext()) {
            Point next = iter.next();
            pointConsumer.accept(next);
        }
    }


    /**
     * This is the data model we are using for the Data Architecture project
     */
    static class NopSegment {

        String hashOfRawData;

        String id;

        String startTime;

        String endTime;

        double lengthInMiles;

        int lengthInSeconds;

        String[] rawData;

        public NopSegment(Track track) {

            String[] rawData = track.asNop().split("\n");

            this.hashOfRawData = HashUtils.hashForStringArray(rawData);

            Instant start = track.startTime();
            Instant end = track.endTime();

            Duration trackTime = Duration.between(start, end);

            this.id = TimeId.newIdFor(start).asBase64();

            this.startTime = start.toString();
            this.endTime = end.toString();

            lengthInSeconds = (int) trackTime.getSeconds();

            List<LatLong> locations = track.points().stream().map(pt -> pt.latLong()).toList();

            lengthInMiles = new LatLongPath(locations).pathDistance().inMiles();

            this.rawData = rawData;
        }
    }
}
