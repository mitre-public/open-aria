package org.mitre.openaria.kafka;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.kafka.KafkaPropertyUtils.verifyKafkaBrokers;

import java.util.Properties;

import org.junit.jupiter.api.Test;

public class KafkaPropertyUtilsTest {

    @Test
    public void verifyKafkaBrokers_happyPath() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092");

        //does nothing when input is valid
        verifyKafkaBrokers(props);
    }

    @Test
    public void verifyKafkaBrokers_happyPath_multipleBrokers() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092,myhost.com:9091");

        //does nothing when input is valid
        verifyKafkaBrokers(props);
    }

    @Test
    public void verifyKafkaBrokers_noPort() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost");

        assertThrows(
            IllegalArgumentException.class,
            () -> verifyKafkaBrokers(props)
        );
    }

    @Test
    public void verifyKafkaBrokers_badPort_multiBroker() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092,myhost.com:notAPort");

        assertThrows(
            IllegalArgumentException.class,
            () -> verifyKafkaBrokers(props)
        );

    }

}
