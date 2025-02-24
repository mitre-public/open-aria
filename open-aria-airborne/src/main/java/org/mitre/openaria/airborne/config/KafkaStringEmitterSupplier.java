package org.mitre.openaria.airborne.config;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;
import static org.mitre.openaria.core.config.YamlUtils.requireMapKeys;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.mitre.openaria.core.config.YamlConfigured;

import org.apache.kafka.clients.producer.KafkaProducer;

public class KafkaStringEmitterSupplier implements Supplier<Consumer<String>>, YamlConfigured {

    boolean wasConfigured = false;

    String topic;

    String kafkaPropFile;

    public KafkaStringEmitterSupplier() {
        //called via Yaml...
    }

    @Override
    public Consumer<String> get() {
        checkState(wasConfigured, "Was not configured, must call configure before getting asset");

        return new org.mitre.openaria.kafka.KafkaStringEmitter(
            topic,
            new KafkaProducer<>(loadProperties(kafkaPropFile))
        );
    }

    @Override
    public void configure(Map<String, ?> configs) {
        this.wasConfigured = true;

        requireMapKeys(configs, "topic", "kafkaPropFile");

        this.topic = (String) configs.get("topic");
        this.kafkaPropFile = (String) configs.get("kafkaPropFile");

        requireNonNull(topic, "The topic was not specified");
        requireNonNull(kafkaPropFile, "The kafkaPropFile was not specified");
    }
}