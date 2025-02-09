package org.mitre.openaria;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.MIN_VALUE;
import static org.mitre.caasd.commons.ConsumingCollections.newConsumingArrayList;
import static org.mitre.openaria.core.utils.Misc.downCastPointIter;

import java.awt.Color;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.mitre.caasd.commons.ConsumingCollections;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.maps.MapBoxApi;
import org.mitre.caasd.commons.maps.MapBuilder;
import org.mitre.caasd.commons.maps.MapFeature;
import org.mitre.caasd.commons.maps.MapFeatures;
import org.mitre.caasd.commons.maps.MonochromeTileServer;
import org.mitre.caasd.commons.maps.TileServer;
import org.mitre.openaria.airborne.tools.TimeDensityAuditor;
import org.mitre.openaria.airborne.tools.TrackStatCollector;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvHit;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvParser;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopParser;
import org.mitre.openaria.system.StreamingKpi;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.io.Files;
import com.google.common.math.Stats;

/**
 * This program ingests a file of vehicle position data, and provides statistics that summarize the
 * data.
 */
public class InspectDataset {

    // stats on time-difference-btw-radar-hits
    // @todo -- add "trackStroke" to command line args
    // @todo -- add "lat" and "lon" to command line args (to set map center)

    static int MAP_WIDTH_IN_PIXELS = 1280;
    static float TRACK_STROKE_WIDTH = 2.0f;


    public static void main(String[] argv) {

        //Use JCommander to parse the CLI args into a useful class
        CommandLineArgs args = parseCommandLineArgs(argv);

        analyzeDataSet(args);
    }

    static void analyzeDataSet(CommandLineArgs args) {

        // Step 1
        // Create a Histogram of the points time using a "TimeDensityAuditor"
        analyzePointDensityOverTime(args);

        // Step 2
        // Prints a Histogram of "Track start times"
        // Prints a Histogram of "Track End times"
        // Prints stats on Track size (e.g. point count)
        // Prints stats on Track Duration (in seconds)
        // Prints stats on Track Points per Minute
        analyzeTracks(args);

        // Step 3
        // Create a Map of the Point data
        makeMapOfPoints(args);
    }

    private static void analyzePointDensityOverTime(CommandLineArgs args) {

        Iterator<Point<?>> pointIterator = pointIteratorFor(args);
        TimeDensityAuditor timeDensityAuditor = new TimeDensityAuditor();
        pointIterator.forEachRemaining(pt -> timeDensityAuditor.accept(pt.time()));

        printSummary(
            timeDensityAuditor,
            "== Histogram of Points per " + timeDensityAuditor.timeBucketSize().getSeconds() + "sec =="
        );
    }

    private static void analyzeTracks(CommandLineArgs args) {

        // Intentionally not using the same point iterator -- want independent results
        Iterator<Point<?>> pointIterator = pointIteratorFor(args);
        TrackStatCollector trackStatCollector = new TrackStatCollector();
        TimeDensityAuditor trackStartDensityAuditor = new TimeDensityAuditor();
        TimeDensityAuditor trackEndDensityAuditor = new TimeDensityAuditor();
        Consumer<Track> combinedConsumer = track -> {
            trackStatCollector.accept(track);
            trackStartDensityAuditor.accept(track.startTime());
            trackEndDensityAuditor.accept(track.endTime());
        };

        StreamingKpi trackKpi = StreamingKpi.trackBasedKpi(combinedConsumer, Duration.ofSeconds(600));
        pointIterator.forEachRemaining(trackKpi);
        trackKpi.flush();

        printSummary(trackStatCollector);
        printSummary(trackStartDensityAuditor, "== Track start times ==");
        printSummary(trackEndDensityAuditor, "== Track end times ==");

    }

    private static void printSummary(TimeDensityAuditor timeDensityAuditor, String titleMessage) {
        var pointsPerTimeBucket = timeDensityAuditor.freqPerTimeBucket();

        System.out.println(titleMessage);
        Iterator<Instant> bucketTimes = timeDensityAuditor.bucketTimeIterator();

        while (bucketTimes.hasNext()) {
            Instant bucketTime = bucketTimes.next();
            System.out.println(bucketTime + ": " + pointsPerTimeBucket.get(bucketTime) + " points");
        }

//        pointsPerTimeBucket.keySet().stream()
//            .sorted()
//            .forEach(bucketTime -> System.out.println(bucketTime + ": " + pointsPerTimeBucket.get(bucketTime) + " points"));
        System.out.println();
    }

    private static void printSummary(TrackStatCollector trackStatCollector) {

        Stats sizeStats = trackStatCollector.trackSizeStats();

        System.out.println("== Statistics on Points per Track ==");
        System.out.println("Num Tracks: " + sizeStats.count());
        System.out.println("Min Track Size: " + (int) sizeStats.min());
        System.out.println("Avg Track Size: " + String.format("%.2f", sizeStats.mean()));
        System.out.println("Max Track Size: " + (int) sizeStats.max());
        System.out.println("StandardDev of Track Size: " + String.format("%.2f", sizeStats.populationStandardDeviation()));
        System.out.println();

        Stats durationStats = trackStatCollector.trackDurationStats();

        System.out.println("== Statistics on Track Duration ==");
        System.out.println("Num Tracks: " + durationStats.count());
        System.out.println("Min Track Duration: " + durationStats.min() + "sec");
        System.out.println("Avg Track Duration: " + String.format("%.2f", durationStats.mean()) + "sec");
        System.out.println("Max Track Duration: " + durationStats.max() + "sec");
        System.out.println("StandardDev of Track Duration: " + String.format("%.2f", durationStats.populationStandardDeviation()));
        System.out.println();

        Stats ptStats = trackStatCollector.pointsPerMinuteStats();

        System.out.println("== Statistics on Track Points Per Minute ==");
        System.out.println("Num Tracks: " + ptStats.count());
        System.out.println("Min Track Points Per Minute: " + String.format("%.2f", ptStats.min()));
        System.out.println("Avg Track Points Per Minute: " + String.format("%.2f", ptStats.mean()));
        System.out.println("Max Track Points Per Minute: " + String.format("%.2f", ptStats.max()));
        System.out.println("StandardDev of Points Per Minute: " + String.format("%.2f", ptStats.populationStandardDeviation()));
        System.out.println();
    }

    private static Iterator<Point<?>> pointIteratorFor(CommandLineArgs args) {

        if (args.parseNop) {
            Iterator<Point<NopHit>> dataIterator = new PointIterator(new NopParser(args.dataFile));
            return downCastPointIter(dataIterator);
        }
        if (args.parseCsv) {
            Iterator<Point<AriaCsvHit>> dataIterator = new AriaCsvParser(args.dataFile);
            return downCastPointIter(dataIterator);
        }

        throw new AssertionError("Logic Error -- No format declared");
    }


    private static void makeMapOfPoints(CommandLineArgs args) {

        // Intentionally not using the same point iterator -- want independent results
        Iterator<Point<?>> pointIterator = pointIteratorFor(args);

        // @todo -- Directly sort the point
        // @todo -- Use TrackMaker directly, skip StreamingKpi
        // If we are forced to keep ALL points and tracks in memory at once there is no upside to using StreamingKpi
//        List<Point<?>> points = Lists.newArrayList(pointIterator);
//        sort(points);

        ConsumingCollections.ConsumingArrayList<Track> aggregator = newConsumingArrayList();

        StreamingKpi trackKpi = StreamingKpi.trackBasedKpi(aggregator, Duration.ofSeconds(600));
        pointIterator.forEachRemaining(trackKpi);
        trackKpi.flush();

        // "aggregator" now holds all the Tracks
        System.out.println("== Making Map of Input Data ==");
        plotMap(aggregator, Files.getNameWithoutExtension(args.dataFile.getName()), args);
        System.out.println("Map Created, see: map-of-" + Files.getNameWithoutExtension(args.dataFile.getName()) + ".png");
    }


    /**
     * Create a map of some input Tracks
     *
     * @param tracks           A list of Tracks
     * @param outputFilePrefix Piece for output filename (e.g. "ROA" --> "map-of-ROA.png")
     */
    public static <T> void plotMap(List<Track> tracks, String outputFilePrefix, CommandLineArgs args) {

        if (tracks.isEmpty()) {
            System.out.println("No tracks to Plot on Map");
            return;
        }

        TileServer tileServer = args.useMapBox
            ? new MapBoxApi(MapBoxApi.Style.DARK)
            : new MonochromeTileServer(Color.BLACK);

        MapBuilder.newMapBuilder()
            .tileSource(tileServer)
            .center(computeAverageLatLong(tracks))
            .width(MAP_WIDTH_IN_PIXELS, args.zoomLevel)
            .addFeatures(tracks, track -> asMapFeature(track, args.trackColor))
            .toFile(new File("map-of-" + outputFilePrefix + ".png"));
    }

    public static <T> LatLong computeAverageLatLong(List<Track> tracks) {

        List<LatLong> locations = tracks.stream()
            .flatMap(track -> asLatLongStream(track))
            .toList();

        return LatLong.avgLatLong(locations.toArray(new LatLong[0]));
    }


    public static MapFeature asMapFeature(Track track, Color c) {

        List<LatLong> locations = asLatLongStream(track).toList();

        return MapFeatures.path(locations, c, TRACK_STROKE_WIDTH);
    }

    private static Stream<LatLong> asLatLongStream(Track<?> track) {
        return track.points().stream().map(point -> point.latLong());
    }


    /** Use JCommander command line argument parser utility to create instances of this class. */
    static class CommandLineArgs {

        @Parameter(names = {"-f", "--file"}, required = true, description = "The file where raw data will be found")
        String fileCmdLineArg;

        @Parameter(names = {"--csv"}, required = false, description = "Use this flag to parse OpenARIA CSV data")
        boolean parseCsv;

        @Parameter(names = {"--nop"}, required = false, description = "Use this flag to parse NOP data")
        boolean parseNop;

        @Parameter(names = {"--map"}, required = false, description = "Use this flag to draw a map of input data")
        boolean shouldDrawMap;

        @Parameter(names = {"--mapBoxTiles"}, required = false, description = "Use this flag add MapBox tiles to a Map (requires an API token)")
        boolean useMapBox;

        @Parameter(names = {"--zoomLevel"}, required = false, description = "The map zoom level")
        int zoomLevel = 7;

        @Parameter(names = {"--red"}, required = false, description = "The Red component of all drawn tracks")
        int red = MIN_VALUE;

        @Parameter(names = {"--green"}, required = false, description = "The Green component of all drawn tracks")
        int green = MIN_VALUE;

        @Parameter(names = {"--blue"}, required = false, description = "The Blue component of all drawn tracks")
        int blue = MIN_VALUE;

        @Parameter(names = {"--alpha"}, required = false, description = "The Alpha component of all drawn tracks")
        int alpha = MIN_VALUE;

        File dataFile;

        Color trackColor;

        //ensure that when JCommander parses all the args the results meet all requirements
        void verifyArguments() {
            checkArgument(parseCsv || parseNop, "Must use --parseCsv OR --parseNop");
            checkArgument(!(parseCsv && parseNop), "Cannot use both --parseCsv AND --parseNop");
            checkArgument(1 <= zoomLevel && zoomLevel <= 18, "zoomLevel must be between 1 and 18");
            checkArgument(0 <= red && red <= 255 || red == MIN_VALUE, "Red must be between 0 and 255");
            checkArgument(0 <= green && green <= 255 || green == MIN_VALUE, "Green must be between 0 and 255");
            checkArgument(0 <= blue && blue <= 255 || blue == MIN_VALUE, "Blue must be between 0 and 255");
            checkArgument(0 <= alpha && alpha <= 255 || alpha == MIN_VALUE, "Alpha must be between 0 and 255");

            //if no "map color info is given use sensible default "Red with middling alpha value"
            if (green == MIN_VALUE && red == MIN_VALUE && blue == MIN_VALUE && alpha == MIN_VALUE) {
                red = 255;
                green = 0;
                blue = 0;
                alpha = 122;
            } else {
                // if any map color is given zero
                red = (red == MIN_VALUE) ? 0 : red;
                green = (green == MIN_VALUE) ? 0 : green;
                blue = (blue == MIN_VALUE) ? 0 : blue;
                alpha = (alpha == MIN_VALUE) ? 122 : alpha;
            }

            //ensure dataFile points us to a viable file
            this.dataFile = new File(fileCmdLineArg);
            checkState(dataFile.isFile());
            checkState(dataFile.exists());

            trackColor = new Color(red, green, blue, alpha);
        }
    }


    /* Use JCommander util to parse the command line args */
    static CommandLineArgs parseCommandLineArgs(String[] args) {

        CommandLineArgs parsedArgs = new CommandLineArgs();
        JCommander.newBuilder()
            .addObject(parsedArgs)
            .build()
            .parse(args);

        parsedArgs.verifyArguments();

        return parsedArgs;
    }
}