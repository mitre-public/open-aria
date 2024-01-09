package org.mitre.openaria.airborne.config;

import java.util.function.Supplier;

import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.caasd.commons.out.OutputSink;
import static org.mitre.caasd.commons.out.Sinks.noOpSink;


public class NoOpOutputSinkSupplier implements Supplier<OutputSink<AirborneEvent>> {

    public NoOpOutputSinkSupplier() {
        //build me using Yaml...
    }

    @Override
    public OutputSink<AirborneEvent> get() {
        return noOpSink();
    }
}
