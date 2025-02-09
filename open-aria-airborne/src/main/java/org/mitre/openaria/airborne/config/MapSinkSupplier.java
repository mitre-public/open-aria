package org.mitre.openaria.airborne.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.config.YamlUtils.requireMapKeys;

import java.util.Map;
import java.util.function.Supplier;

import org.mitre.openaria.core.config.YamlConfigured;


public class MapSinkSupplier implements Supplier<MapSink>, YamlConfigured {

    private boolean wasConfigured = false;

    private String eventDirectory;

    private boolean useMapBox;

    private double mapWidthInNauticalMiles;

    public MapSinkSupplier() {
        //called via Yaml...
    }

    @Override
    public MapSink get() {
        checkState(wasConfigured, "Was not configured, must call configure before getting asset");

        return new MapSink(
            eventDirectory,
            useMapBox,
            mapWidthInNauticalMiles
        );
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.wasConfigured = true;

        requireMapKeys(configs, "eventDirectory");

        this.eventDirectory = (String) configs.get("eventDirectory");
        this.useMapBox = (boolean) configs.get("useMapBox");
        this.mapWidthInNauticalMiles = (double) configs.get("mapWidthInNauticalMiles");

        requireNonNull(eventDirectory, "The eventDirectory was not specified");
    }
}
