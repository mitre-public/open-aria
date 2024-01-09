package org.mitre.openaria.airborne.config;

import java.util.function.Supplier;

import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.caasd.commons.out.PrintStreamSink;

public class StdOutSinkSupplier implements Supplier<PrintStreamSink<AirborneEvent>> {

    public StdOutSinkSupplier() {
        //build me using Yaml...
    }

    @Override
    public PrintStreamSink<AirborneEvent> get() {
        return new PrintStreamSink<>(AirborneEvent::asJson, System.out);
    }
}
