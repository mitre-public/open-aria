package org.mitre.openaria.kafka;

import java.util.function.Consumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * A KafkaStringEmitter is a simple String Consumer that sends incoming String data to a Kafka topic
 * (all Kafka messages have no Key, no partition, and no KafkaHeader).
 */
public class KafkaStringEmitter implements Consumer<String> {

    private final KafkaEmitter<String> emitter;

    public KafkaStringEmitter(String targetTopic, KafkaProducer<String, String> kafkaProducer) {
        this.emitter = new KafkaEmitter<>(emitToTopic(targetTopic), kafkaProducer);
    }

    @Override
    public void accept(String t) {
        emitter.accept(t);
    }

    /**
     * This Factory makes ProducerRecords that route data to the single topic. This is a very simple
     * implementation that does not set a Partition, a key, or a KafkaHeader information.
     */
    private static ProducerRecordFactory<String, String, String> emitToTopic(final String topic) {
        return (String messageForKafka) -> new ProducerRecord(topic, messageForKafka);
    }
}
