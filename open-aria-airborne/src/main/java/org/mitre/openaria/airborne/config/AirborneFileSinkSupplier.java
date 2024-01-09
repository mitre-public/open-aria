package org.mitre.openaria.airborne.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.config.YamlUtils.requireMapKeys;

import java.util.Map;
import java.util.function.Supplier;

import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.core.config.YamlConfigured;
import org.mitre.caasd.commons.out.JsonFileSink;

public class AirborneFileSinkSupplier implements Supplier<JsonFileSink<AirborneEvent>>, YamlConfigured {

    private boolean wasConfigured = false;

    private String eventDirectory;

    public AirborneFileSinkSupplier() {
        //called via Yaml...
    }

    @Override
    public JsonFileSink<AirborneEvent> get() {
        checkState(wasConfigured, "Was not configured, must call configure before getting asset");

        return new JsonFileSink<>(
            eventDirectory,
            AirborneEvent::nameFile
        );
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.wasConfigured = true;

        requireMapKeys(configs, "eventDirectory");

        this.eventDirectory = (String) configs.get("eventDirectory");

        requireNonNull(eventDirectory, "The eventDirectory was not specified");
    }
}
