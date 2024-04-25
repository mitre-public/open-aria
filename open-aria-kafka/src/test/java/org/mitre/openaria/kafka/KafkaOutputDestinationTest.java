package org.mitre.openaria.kafka;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import org.mitre.caasd.commons.out.JsonWritable;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;

public class KafkaOutputDestinationTest {

    Properties producerProperties() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "0");
        props.put("max.block.ms", "50");  //how long the kafkaProducer.send() method blocks when topic metadata is unavailable.
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("compression.type", "none");
        props.put("retries", "0");
        props.put("batch.size", "1024");
        props.put("linger.ms", "100");

        return props;
    }

    static class DataClassThatCanBecomeJson implements JsonWritable {

        public final String key;

        public final int importantValue;

        public DataClassThatCanBecomeJson(String key, int value) {
            this.key = key;
            this.importantValue = value;
        }
    }

    static class SimpleProducerRecordFactory implements ProducerRecordFactory<String, String, DataClassThatCanBecomeJson> {

        @Override
        public ProducerRecord<String, String> producerRecordFor(DataClassThatCanBecomeJson itemForKafka) {
            return new ProducerRecord<>(
                "targetTopic", //these items ALWAYS go to this topic
                0, //these items ALWAYS go to partition 0
                itemForKafka.key,
                itemForKafka.asJson()
            );
        }
    }

    /**
     * This test SHOULD throw an exception because we are sending a message to Kafka when Kafka is
     * NOT up and running. Consequently, we want some sort of Exception to be thrown here.
     */
    @Test
    public void kafkaEventDestinationGeneratesWarningsWhenWritesFail() {

        Properties props = producerProperties();

        KafkaOutputSink<DataClassThatCanBecomeJson> kafkaWriter = new KafkaOutputSink<>(
            new SimpleProducerRecordFactory(),
            new KafkaProducer<String, String>(props)
        );

        DataClassThatCanBecomeJson sendMeToKafka = new DataClassThatCanBecomeJson("myKey", 123);

        assertThrows(
            KafkaException.class,
            () -> kafkaWriter.accept(sendMeToKafka)
        );

    }

    @Test
    public void kafkaEventDestinationSendsRecordCorrectly() {

        Properties props = producerProperties();
        SpyKafkaProducer spyProducer = new SpyKafkaProducer(props);

        KafkaOutputSink<DataClassThatCanBecomeJson> kafkaWriter = new KafkaOutputSink<>(
            new SimpleProducerRecordFactory(),
            spyProducer
        );

        DataClassThatCanBecomeJson sendMeToKafka = new DataClassThatCanBecomeJson("myKey", 123);

        kafkaWriter.accept(sendMeToKafka);

        //the KafkaProducer DID indeed recieve a ProducerRecord
        assertThat(spyProducer.records.size(), is(1));
    }

    /**
     * This KafkaProducer doesn't do anything with records it received besides store them locally
     * for querying later (by test methods).
     */
    private static class SpyKafkaProducer extends KafkaProducer<String, String> {

        private final List<ProducerRecord<String, String>> records;

        public SpyKafkaProducer(Properties properties) {
            super(properties);
            records = new ArrayList<>(1);
        }

        @Override
        public Future<RecordMetadata> send(ProducerRecord<String, String> record) {
            return send(record, null);
        }

        @Override
        public Future<RecordMetadata> send(ProducerRecord<String, String> record, Callback callback) {
            records.add(record);
            return null;
        }
    }
}
