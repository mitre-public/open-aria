package org.mitre.openaria.airborne;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.mitre.caasd.commons.util.PropertyUtils.getString;
import static org.mitre.openaria.kafka.FacilityPartitionMapping.parseFacilityMappingFile;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.mitre.openaria.core.formats.nop.Facility;
import org.mitre.openaria.kafka.FacilityPartitionMapping;
import org.mitre.openaria.system.FacilitySet;

import com.google.common.collect.Lists;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

/**
 * An AirborneEventGrabber extracts data from a Kafka Topic that contains Airborne events (stored as
 * JSON). An AirborneEventGrabber only extracts events that existed in Kafka when the Grabber was
 * created. Consequently, an AirborneEventGrabber IS NOT meant to continuously stream events.
 */
public class AirborneEventGrabber {

    /* Property files that configure a AirborneEventGrabber MUST have this property defined. */
    private static final String NAME_OF_EVENT_TOPIC = "eventTopic";

    /* Property files that configure a AirborneEventGrabber MUST have this property defined. */
    public static final String KAFKA_PARTITION_MAPPING_FILE = "kafkaPartitionMappingFile";

    /** The set of facilities that will have their events pulled. */
    private final FacilitySet facilitySet;

    /**
     * This KafkaConsumer is configured to retrieve data for all Facilties in the FaciltySet.
     * Retrieving new Point data in "large requests" (that polls data for multiple facilities) is
     * far more efficient than making many "small requests" (i.e. that only poll data for exactly 1
     * facility).
     *
     * <p>The keys are String (ignored), The values of CompleteEvents encoded as JSON
     */
    private final KafkaConsumer<String, String> kafkaConsumer;

    private final FacilityPartitionMapping facilityMapping;

    private final List<TopicPartition> topicPartitions;

    private final Map<TopicPartition, Long> startingOffsets;

    private final Map<TopicPartition, Long> maxOffsets;

    /**
     * Create an AirborneEventGrabber.
     *
     * @param consumerProperties
     * @param facilitySet
     */
    public AirborneEventGrabber(Properties consumerProperties, FacilitySet facilitySet) {
        checkNotNull(consumerProperties);
        checkNotNull(facilitySet);
        checkArgument(!facilitySet.isEmpty(), "Do not consume an empty FacilitySet");

        String topicName = getString(NAME_OF_EVENT_TOPIC, consumerProperties);
        File partitionMappingFile = new File(getString(KAFKA_PARTITION_MAPPING_FILE, consumerProperties));
        this.facilityMapping = parseFacilityMappingFile(partitionMappingFile);
        this.facilitySet = removeUnpartitionedFacilities(facilitySet, facilityMapping);
        this.topicPartitions = listOfPartitionsToConsume(topicName);
        this.kafkaConsumer = createAndInitalizeKafkaConsumer(consumerProperties);
        this.startingOffsets = kafkaConsumer.beginningOffsets(topicPartitions);
        this.maxOffsets = kafkaConsumer.endOffsets(topicPartitions);

        System.out.println(kafkaContentsByFacility());
    }

    /**
     * Create a revised FacilitySet that does not contain any facilities that are unavailable in the
     * provided FacilityPartitionMapping.
     */
    private FacilitySet removeUnpartitionedFacilities(FacilitySet facilities, FacilityPartitionMapping partitionMapping) {
        HashSet<Facility> filteredSet = newHashSet();

        for (Facility facility : facilities) {
            if (partitionMapping.hasPartitionFor(facility)) {
                filteredSet.add(facility);
            } else {
                //this is intended
                System.out.println("Ignoring facility: " + facility + " because it was not mapped to a Kafka Partition");
            }
        }

        return new FacilitySet(filteredSet);
    }

    private KafkaConsumer<String, String> createAndInitalizeKafkaConsumer(Properties kafkaConsumerProps) {

        KafkaConsumer<String, String> kc = new KafkaConsumer<>(kafkaConsumerProps);
        kc.assign(topicPartitions);
        kc.seekToBeginning(topicPartitions);

        return kc;
    }

    private List<TopicPartition> listOfPartitionsToConsume(String topicName) {

        List<TopicPartition> partitionsToConsumer = Lists.newArrayList();

        for (Facility facility : this.facilitySet) {
            TopicPartition tp = new TopicPartition(topicName, facilityMapping.partitionFor(facility).get());
            partitionsToConsumer.add(tp);
        }
        System.out.println("Retrieving events from: " + partitionsToConsumer.size() + " partitions");

        return partitionsToConsumer;
    }

    public ArrayList<AirborneEvent> getEvents() {

        Duration timeout = Duration.ofMillis(15_000);

        System.out.println("polling..");
        ConsumerRecords<String, String> records = kafkaConsumer.poll(timeout);
        System.out.println("done polling [" + records.count() + " records retrieved]");

        ArrayList<AirborneEvent> events = newArrayList();

        for (ConsumerRecord<String, String> record : records) {

            TopicPartition tp = new TopicPartition(record.topic(), record.partition());

            if (record.offset() < maxOffsets.get(tp)) {
                AirborneEvent event = AirborneEvent.parseJson(record.value());
                events.add(event);
            }
        }

        System.out.println("Pulled: " + events.size() + " CompleteEvents");

        return events;
    }

    public String kafkaContentsByFacility() {

        StringBuilder sb = new StringBuilder();

        long totalCount = 0;

        for (TopicPartition topicPartition : this.topicPartitions) {
            long count = this.maxOffsets.get(topicPartition) - this.startingOffsets.get(topicPartition);
            totalCount += count;

            Facility facility = facilityMapping.facilityFor(topicPartition.partition()).get();
            sb.append(facility).append(" has ").append(count).append(" records in Kafka\n");
        }

        sb.append("In total ").append(totalCount).append(" records are in Kafka\n");

        return sb.toString();
    }
}
