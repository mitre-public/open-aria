
package org.mitre.openaria.publishing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.mitre.caasd.commons.util.PropertyUtils.getBoolean;
import static org.mitre.caasd.commons.util.PropertyUtils.getString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InvalidTopicException;

/**
 * A ContinuousEventGrabber extracts data from a Kafka Topic that contains AirborneEvents.
 * <p>
 * A ContinuousEventGrabber has no stopping condition. Additionally, it can be configured to
 * beginning pulling data at the current offset or at the oldest available offset.
 */
public class ContinuousEventGrabber implements Supplier<Collection<String>> {

    /* Property files that configure a ContinuousEventGrabber MUST have this property defined. */
    private static final String NAME_OF_EVENT_TOPIC = "eventTopic";

    private static final String SEEK_TO_BEGINNING = "startFromBeginning";

    private final KafkaConsumer<String, String> kafkaConsumer;

    /**
     * Continuously retrieve events from Kafka and process each event with the provided Consumer
     *
     * @param properties Configures the KafkaConsumer
     */
    public ContinuousEventGrabber(Properties properties) {
        checkNotNull(properties);

        this.kafkaConsumer = createAndInitializeKafkaConsumer(properties);
    }

    private KafkaConsumer<String, String> createAndInitializeKafkaConsumer(Properties props) {

        String topic = getString(NAME_OF_EVENT_TOPIC, props);
        boolean startFromBeginning = getBoolean(SEEK_TO_BEGINNING, props);

        KafkaConsumer<String, String> kc = new KafkaConsumer(props);

        if (startFromBeginning) {
            //create list of all the partition in the desired topic...
            List<TopicPartition> tps = newArrayList();
            List<PartitionInfo> topicPartitions = kc.listTopics().computeIfAbsent(topic, key -> {
                throw new InvalidTopicException(String.format("%s not found in topic list. It doesn't exist or you may not have access", topic));
            });

            for (PartitionInfo partitionInfo : topicPartitions) {
                tps.add(new TopicPartition(topic, partitionInfo.partition()));
            }

            //begin consuming at the beginning of these topics
            kc.assign(tps);
            kc.seekToBeginning(tps);
        } else {
            kc.subscribe(newArrayList(topic));
        }

        return kc;
    }

    @Override
    public Collection<String> get() {
        /*
         * This is arguably not a good design because it hides information contained in the
         * ConsumerRecords. Most notably, the offsets and partition numbers are lost here.
         *
         * More over, the data from multiple partitions get intermingled in one list -- this could
         * be very bad if that mixing cannot be undone.
         *
         * However, the main goal of this class is to remove this complexity so downstream users
         * have an easier API to work with.
         */
        Duration timeout = Duration.ofMillis(15_000);

        System.out.println("polling..");
        ConsumerRecords<String, String> records = kafkaConsumer.poll(timeout);
        System.out.println("done polling [" + records.count() + " records retrieved]");

        ArrayList<String> events = newArrayList();

        for (ConsumerRecord<String, String> record : records) {
            events.add(record.value());
        }

        return events;
    }
}
