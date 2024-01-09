package org.mitre.openaria.airborne.config;

import static com.google.common.base.Preconditions.checkState;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.config.YamlUtils.requireMapKeys;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.mitre.openaria.core.config.YamlConfigured;
import org.mitre.openaria.system.FileSink;
import org.mitre.caasd.commons.Functions;
import org.mitre.caasd.commons.YyyyMmDd;

public class FileSinkSupplier implements Supplier<Consumer<String>>, YamlConfigured {

    private boolean wasConfigured = false;

    private String logDir;

    public FileSinkSupplier() {
        //called via YAML...
    }

    @Override
    public FileSink get() {
        checkState(wasConfigured, "Was not configured, must call configure before getting asset");

        //Automatically break up the log files by day
        //Log Files will get names like "2021-02-15-log.txt"
        Functions.ToStringFunction<String> fileNamer = (logMessage) -> (YyyyMmDd.from(now()).toString()) + "-log";

        return new FileSink(logDir, fileNamer);
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.wasConfigured = true;

        requireMapKeys(configs, "logDir");

        this.logDir = (String) configs.get("logDir");

        requireNonNull(logDir, "The logDir was not specified");
    }
}