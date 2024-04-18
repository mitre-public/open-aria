package org.mitre.openaria;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;
import static org.mitre.openaria.core.config.YamlUtils.parseYaml;
import static org.mitre.openaria.kafka.FacilityPartitionMapping.parseFacilityMappingFile;
import static org.mitre.openaria.system.ExceptionHandlers.sequentialFileWarner;
import static org.mitre.openaria.system.KafkaIngestor.nopPlugin;

import java.io.File;
import java.time.Duration;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.mitre.caasd.commons.parsing.nop.Facility;
import org.mitre.openaria.airborne.metrics.KpiLogger;
import org.mitre.openaria.core.config.PluginFactory;
import org.mitre.openaria.kafka.FacilityPartitionMapping;
import org.mitre.openaria.system.KafkaIngestor;
import org.mitre.openaria.system.tools.DataLatencySummarizer;
import org.mitre.openaria.system.tools.KafkaLatencyLogger;
import org.mitre.openaria.system.tools.SwimLaneLogger;

public class RunAirborneOnKafkaNop {

    /**
     * Example Usage: java -cp ARIA.jar org.mitre.openaria.RunAirborneOnKafkaNop config.yaml
     *
     * <p>This is a long-running program does not terminate.  The executable start several long
     * lived threads (in its executor) that continually pull data from Kafka and process it.
     */
    public static void main(String[] args) throws Exception {
        checkArgument(args.length == 1, "One command line argument is required: The properties file");

        Config config = configFromYaml(new File(args[0]));

        AirborneFactory factory = config.airFactory();

        KafkaIngestor<String, Facility> system = new KafkaIngestor<>(
            config.options(),
            config.inputKafkaProps(),
            factory::createKpi,
            config.partitionMap(),
            nopPlugin(),
            DataLatencySummarizer.byPartitionSummarizer(),
            sequentialFileWarner("warnings")
        );

        Consumer<String> logDest = config.logMessageReceiver();

        //Schedule regular collection and publication of data that looks like...
        //{"logType":"FINDINGS","time":"2021-02-22T14:50:15Z","facility":"A80","events":6,"points":88768,"tracks":2573,"trackPairs":9519}
        //{"logType":"SEQ_AUDIT","facility":"A80","points":88768,"droppedPoints":0,"ptTimeChangeSec":4032,"avgLatencyDroppedPtsSec":0,"fracPtsDropped":0.0}
        system.scheduleQuickTask(
            new KpiLogger(factory.hostId(), factory.streamingKpis(), logDest),
            config.loggingPeriod()
        );

        //Schedule regular collection and publication of Kafka Latency data that looks like...
        //{"logType":"LATENCY","time":"2021-02-23T18:32:00Z","facility":"GSO","points":32426,"avgConsumeLatencyMs":2034860,"avgUploadLatencyMs":15528764670}
        system.scheduleQuickTask(
            new KafkaLatencyLogger<>(factory.hostId(), system.latencyCollectors(), logDest),
            config.loggingPeriod()
        );

        //Schedule regular collection and publication of SwimLane throughput data that looks like...
        //{"logType":"BUFFER","facility":"SYR","hostId":"10.22.12.230","time":"2023-06-28T14:03:14Z","numPointsIngested":8050,"numPointsProcessed":14003,"numPointsDelta":-5953}
        system.scheduleQuickTask(
                new SwimLaneLogger<>(factory.hostId(), system.swimLanes(), logDest),
                config.loggingPeriod()
        );
    }

    public static Config configFromYaml(File yamlFile) throws Exception {
        return parseYaml(yamlFile, RunAirborneOnKafkaNop.Config.class);
    }

    public static class Config {
        String inputKafkaPropFile;

        String kafkaPartitionMappingFile;

        AirborneFactory.Builder airborneConfig;

        KafkaIngestor.Options dataProcessingOptions;

        int loggingPeriodSec;

        //Expects classes that implement Supplier<Consumer<String>>
        PluginFactory[] logSinkSuppliers;

        public Config() {
            //build with Yaml...
        }

        public Properties inputKafkaProps() {
            return loadProperties(new File(inputKafkaPropFile));
        }

        public FacilityPartitionMapping partitionMap() {
            return parseFacilityMappingFile(new File(kafkaPartitionMappingFile));
        }

        public AirborneFactory airFactory() {
            return this.airborneConfig.build();
        }

        public KafkaIngestor.Options options() {
            return this.dataProcessingOptions;
        }

        public Duration loggingPeriod() {
            return Duration.ofSeconds(loggingPeriodSec);
        }

        public Consumer<String> logMessageReceiver() {
            /*
             * Use the PluginFactory to build one (or more) Supplier<Consumer<String>>
             * Then use the suppliers and links the resulting Consumer<String> into one composite Consumer<String>
             */
            requireNonNull(logSinkSuppliers, "logSinkSuppliers cannot be null, log messages must be handled");

            Consumer<String> combinedSink = null;

            for (PluginFactory ifc : logSinkSuppliers) {
                Supplier<Consumer<String>> supplier = ifc.createConfiguredInstance(Supplier.class);

                Consumer<String> sink = supplier.get();

                combinedSink = combinedSink != null
                    ? combinedSink.andThen(sink)
                    : sink;
            }
            return combinedSink;
        }
    }
}
