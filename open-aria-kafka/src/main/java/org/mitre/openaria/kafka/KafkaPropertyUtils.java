package org.mitre.openaria.kafka;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.mitre.caasd.commons.util.PropertyUtils.getString;

import java.util.Properties;

public class KafkaPropertyUtils {

    /**
     * Verify this Properties object will not cause KafkaProducer's constructor to fail due to
     * missing a REQUIRED property.
     *
     * @param props A Properties object that will be provided to the constructor of a KafkaProducer
     */
    public static void verifyKafkaProducerProperties(Properties props) {
        getString("bootstrap.servers", props);
        getString("compression.type", props);
        getString("acks", props);
        getString("retries", props);
        getString("batch.size", props);
        getString("linger.ms", props);
        getString("key.serializer", props);
        getString("value.serializer", props);
    }

    /**
     * Verify this Properties object will not cause KafkaConsumer's constructor to fail due to
     * missing a REQUIRED property.
     *
     * @param props A Properties object that will be provided to the constructor of a KafkaConsumer
     */
    public static void verifyKafkaConsumerProperties(Properties props) {
        getString("bootstrap.servers", props);
        getString("key.deserializer", props);
        getString("value.deserializer", props);
    }

    /**
     * Verify this Properties object contains a "bootstrap.servers" value that matches Kafka's
     * expected format for 1 or more brokers (e.g. "host1:port,host2:port,host3:port")
     *
     * @param props A Properties object that will be provided to a KafkaConsumer or KafkaProducer
     *              constructor
     */
    public static void verifyKafkaBrokers(Properties props) {
        //ensure bootstrap.servers is assigned
        String brokerList = getString(BOOTSTRAP_SERVERS_CONFIG, props); //usually = "bootstrap.servers"

        String[] brokers = brokerList.split(",");

        for (String broker : brokers) {
            checkArgument(
                broker.contains(":"),
                "Proper broker formatting requires a \":\" between the host and the port (input=" + broker + ")"
            );
            String host = broker.substring(0, broker.indexOf(":"));  //we could validate the host is we wanted to
            String port = broker.substring(broker.indexOf(":") + 1);

            parseInt(port);//every port should be an integer
        }
    }
}
