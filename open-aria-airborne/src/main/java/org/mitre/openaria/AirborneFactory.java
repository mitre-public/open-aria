package org.mitre.openaria;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.airborne.AirborneAria.airborneAria;
import static org.mitre.openaria.system.StreamingKpi.trackPairKpi;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

import org.mitre.openaria.airborne.AirborneAlgorithmDef;
import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.airborne.OutputConfig;
import org.mitre.openaria.core.formats.Format;
import org.mitre.openaria.core.formats.nop.Facility;
import org.mitre.openaria.pointpairing.PairingConfig;
import org.mitre.openaria.system.KpiFactory;
import org.mitre.openaria.system.StreamingKpi;

/**
 * An AirborneFactory creates AirbornePairConsumers -- its role is to ensure (1) all KPI's deployed
 * will have the same configuration and (2) certain assets (like data caches, output loggers, and
 * output emitters) are shared between all KPI's that get deployed.
 */
public class AirborneFactory implements KpiFactory<Facility> {

    private final AirborneAlgorithmDef algorithmDef;

    private final Consumer<AirborneEvent> sharedDownstream;

    private final double trackPairingDistanceInNM;

    private final int inMemorySortBufferSec;

    /*
     * Retain a map of all the StreamingKpi created, and their corresponding Facility, so we can
     * extract the data we need to log Point, Track, and Event level data
     */
    private final Map<Facility, StreamingKpi<AirbornePairConsumer>> kpisCreated;

    /**
     * This Constructor is called by the Builder (which is Built via parsing YAML)
     *
     * @param algorithmDef             An algorithm definition built from a yaml component
     * @param outputConfig             A yaml component that can produce the series of OutputSinks
     *                                 we'll need
     * @param trackPairingDistanceInNM How close aircraft have to get to be paired an analyzed
     * @param inMemorySortBufferSec    How much data is kept in-memory before it gets processed.
     *                                 This value impacts latency, memory requirements, and output
     *                                 stability.
     */
    AirborneFactory(
        AirborneAlgorithmDef algorithmDef,
        OutputConfig outputConfig,
        double trackPairingDistanceInNM,
        int inMemorySortBufferSec
    ) {
        this.algorithmDef = algorithmDef;
        this.sharedDownstream = outputConfig.outputSink();
        this.kpisCreated = newHashMap();
        this.trackPairingDistanceInNM = trackPairingDistanceInNM;
        this.inMemorySortBufferSec = inMemorySortBufferSec;
    }

    @Override
    public StreamingKpi<AirbornePairConsumer> createKpi(Facility facility) {

        //Create a new AirborneProperties -- always reusing the assets that should not be replicated
        AirbornePairConsumer airborne = new AirbornePairConsumer(
            airborneAria(algorithmDef),
            sharedDownstream
        );

        StreamingKpi<AirbornePairConsumer> kpi = trackPairKpi(
            airborne,
            new PairingConfig(Duration.ofSeconds(13), trackPairingDistanceInNM),
            inMemorySortBufferSec
        );

        this.kpisCreated.put(facility, kpi);

        return kpi;
    }

    public Map<Facility, StreamingKpi<AirbornePairConsumer>> streamingKpis() {
        return kpisCreated;
    }

    public Format<?> format() {
        // @todo -- Can we make this whole class Generic in the DataFormat type?
        return this.algorithmDef.dataFormat();
    }

    public String hostId() {
        return this.algorithmDef.hostId();
    }

    /** This Builder is designed to be instantiated by a Yaml file. */
    public static class Builder {
        AirborneAlgorithmDef.Builder algorithmDef;

        OutputConfig.Builder outputConfig;

        Double trackPairingDistanceInNM;
        Integer inMemorySortBufferSec;

        public AirborneFactory build() {
            requireNonNull(algorithmDef);
            requireNonNull(outputConfig);
            requireNonNull(trackPairingDistanceInNM);
            requireNonNull(inMemorySortBufferSec);

            //As per documentation on YAML ...

            return new AirborneFactory(
                algorithmDef.build(),
                outputConfig.build(),
                trackPairingDistanceInNM,
                inMemorySortBufferSec
            );
        }
    }
}
