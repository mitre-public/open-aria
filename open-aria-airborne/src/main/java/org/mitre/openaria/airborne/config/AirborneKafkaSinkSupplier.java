package org.mitre.openaria.airborne.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.config.YamlUtils.requireMapKeys;
import static org.mitre.openaria.kafka.FacilityPartitionMapping.parseFacilityMappingFile;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.mitre.openaria.airborne.AirborneEvent;
import org.mitre.openaria.airborne.AirborneProducerRecordFactory;
import org.mitre.openaria.core.config.YamlConfigured;
import org.mitre.openaria.kafka.KafkaOutputSink;

/** Designed to be created by parsing a yaml file */
public class AirborneKafkaSinkSupplier implements Supplier<KafkaOutputSink<AirborneEvent>>, YamlConfigured {

    boolean wasConfigured = false;

    String kafkaPartitionMappingFile;

    String topic;

    String kafkaPropFile;

    public AirborneKafkaSinkSupplier() {
        //called via Yaml...
    }

    @Override
    public KafkaOutputSink<AirborneEvent> get() {
        checkState(wasConfigured, "Was not configured, must call configure before getting asset");

        File mapping = new File(kafkaPartitionMappingFile);
        checkState(mapping.exists(), "Could not find the kafkaPartitionMappingFile: " + kafkaPartitionMappingFile);
        Properties props = loadProperties(kafkaPropFile);

        return new KafkaOutputSink<AirborneEvent>(
            new AirborneProducerRecordFactory(topic, parseFacilityMappingFile(mapping)),
            new KafkaProducer<String, String>(props)
        );
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.wasConfigured = true;

        requireMapKeys(configs, "topic", "kafkaPropFile", "kafkaPartitionMappingFile");

        this.topic = (String) configs.get("topic");
        this.kafkaPropFile = (String) configs.get("kafkaPropFile");
        this.kafkaPartitionMappingFile = (String) configs.get("kafkaPartitionMappingFile");

        requireNonNull(topic, "The topic was not specified");
        requireNonNull(kafkaPropFile, "The kafkaPropFile was not specified");
        requireNonNull(kafkaPartitionMappingFile, "The kafkaPartitionMappingFile was not specified");
    }
}