package org.mitre.openaria.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;


/**
 * A ProducerRecordFactory generates ProducerRecords that can be sent directly to Kafka.
 *
 * @param <K> The Key class used in Kafka
 * @param <V> The Value class used in Kafka
 * @param <T> The type of data we want to "send to Kafka", this will be different from V if a
 *            serialization scheme is used.
 */
@FunctionalInterface
public interface ProducerRecordFactory<K, V, T> {

    ProducerRecord<K, V> producerRecordFor(T itemForKafka);
}
