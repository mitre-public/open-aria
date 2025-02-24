package org.mitre.openaria.airborne.config;

import static com.google.common.base.Preconditions.checkArgument;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.maps.MapFeatures.path;

import java.awt.Color;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.maps.MapBuilder;
import org.mitre.caasd.commons.maps.MapFeatures;
import org.mitre.caasd.commons.out.OutputSink;
import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.core.TrackPair;


/**
 * A MapSink is Consumer that accepts AirborneEvents and draws maps of them.
 * <p>
 * MapSink can be added to ARIA's output configuration using the sister class MapSinkSupplier
 */
public class MapSink implements OutputSink<AirborneEvent> {

    static float TRACK_STROKE_WIDTH = 2.0f;

    private final String outputDirectory;

    private final boolean useMapBox;

    private final double mapWidthInNauticalMiles;

    /**
     * Create a MapSink that: writes to "eventMaps", does not use MapBox tiles, and shows the 15
     * nautical miles around an event location.
     */
    public MapSink() {
        this("eventMaps", false, 15.0);
    }

    public MapSink(String outputDirectory, boolean useMapBox, double mapWidthInNauticalMiles) {
        requireNonNull(outputDirectory);
        checkArgument(!outputDirectory.isEmpty());
        checkArgument(1.0 <= mapWidthInNauticalMiles && mapWidthInNauticalMiles <= 25.0, "Map width 1.0 and 25.0");

        this.outputDirectory = outputDirectory;
        this.useMapBox = useMapBox;
        this.mapWidthInNauticalMiles = mapWidthInNauticalMiles;
    }


    @Override
    public void accept(AirborneEvent airborneEvent) {

        prepareTargetDir();

        // get track data... make map ...
        TrackPair<?> tracks = airborneEvent.rawTracks();

        // Create an unfinished MapBuilder ... configure the tile server and tile caching ...
        MapBuilder partialBuilder = useMapBox
            ? MapBuilder.newMapBuilder().mapBoxDarkMode().useLocalDiskCaching(Duration.ofDays(30))
            : MapBuilder.newMapBuilder().solidBackground(Color.BLACK);

        // Finish the MapBuilding process ...
        partialBuilder
            .center(airborneEvent.latLong())
            .width(Distance.ofNauticalMiles(mapWidthInNauticalMiles))
            .addFeature(MapFeatures.filledCircle(airborneEvent.latLong(), Color.MAGENTA, 12))
            .addFeature(path(tracks.track1().pointLatLongs(), RED, TRACK_STROKE_WIDTH))
            .addFeature(path(tracks.track2().pointLatLongs(), BLUE, TRACK_STROKE_WIDTH))
            .toFile(new File(outputDirectory, "map-of-" + airborneEvent.uuid() + ".png"));
    }

    private void prepareTargetDir() {

        Path targetDir = Paths.get(outputDirectory);
        targetDir.toFile().mkdirs();
    }
}