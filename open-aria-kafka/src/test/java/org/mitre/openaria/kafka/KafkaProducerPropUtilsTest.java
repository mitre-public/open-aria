package org.mitre.openaria.kafka;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.kafka.KafkaPropertyUtils.verifyKafkaConsumerProperties;
import static org.mitre.openaria.kafka.KafkaPropertyUtils.verifyKafkaProducerProperties;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.util.PropertyUtils;

public class KafkaProducerPropUtilsTest {

    private static Properties minimalProducerProps() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost/:9092");
        props.setProperty("compression.type", "none");
        props.setProperty("acks", "1");
        props.setProperty("retries", "0");
        props.setProperty("batch.size", "16384");
        props.setProperty("linger.ms", "0");
        props.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        return props;
    }

    @Test
    public void testMinimalCase() {
        verifyKafkaProducerProperties(minimalProducerProps());
    }

    @Test
    public void testMissingKeySerializer() {
        Properties props = minimalProducerProps();
        props.remove("key.serializer");

        assertThrows(
            PropertyUtils.MissingPropertyException.class,
            () -> verifyKafkaProducerProperties(props)
        );
    }

    @Test
    public void testMissingValueSerializer() {
        Properties props = minimalProducerProps();
        props.remove("value.serializer");

        assertThrows(
            PropertyUtils.MissingPropertyException.class,
            () -> verifyKafkaProducerProperties(props)
        );
    }

    @Test
    public void testMissingCompression() {
        Properties props = minimalProducerProps();
        props.remove("compression.type");

        assertThrows(
            PropertyUtils.MissingPropertyException.class,
            () -> verifyKafkaProducerProperties(props)
        );
    }

    @Test
    public void testMissingBootstrapServer_producer() {
        Properties props = minimalProducerProps();
        props.remove("bootstrap.servers");

        assertThrows(
            PropertyUtils.MissingPropertyException.class,
            () -> verifyKafkaProducerProperties(props)
        );
    }

    private static Properties minimalConsumerProps() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost/:9092");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringSerializer");

        return props;
    }

    @Test
    public void testMissingKeyDeserializer() {
        Properties props = minimalConsumerProps();
        props.remove("key.deserializer");

        assertThrows(
            PropertyUtils.MissingPropertyException.class,
            () -> verifyKafkaConsumerProperties(props)
        );
    }

    @Test
    public void testMissingValueDeserializer() {
        Properties props = minimalConsumerProps();
        props.remove("value.deserializer");

        assertThrows(
            PropertyUtils.MissingPropertyException.class,
            () -> verifyKafkaConsumerProperties(props)
        );
    }

    @Test
    public void testMissingBootstrapServer_consumer() {
        Properties props = minimalConsumerProps();
        props.remove("bootstrap.servers");

        assertThrows(
            PropertyUtils.MissingPropertyException.class,
            () -> verifyKafkaConsumerProperties(props)
        );
    }
}
