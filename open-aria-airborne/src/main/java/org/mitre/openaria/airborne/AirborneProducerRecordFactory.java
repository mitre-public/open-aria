package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.mitre.openaria.kafka.FacilityPartitionMapping;
import org.mitre.openaria.kafka.ProducerRecordFactory;

/**
 * An AirborneProducerRecordFactory is responsible for making Kafka ProducerRecords whenever an
 * AirborneEvent needs to be written to Kafka. This task requires knowing the Kafka partition
 * mapping and knowing which Headers are expected by Kafka (as per an agreement with the FAA EIM
 * team)
 */
public class AirborneProducerRecordFactory implements ProducerRecordFactory<String, String, AirborneEvent> {

    private final String targetTopic;
    private final FacilityPartitionMapping mapping;

    public AirborneProducerRecordFactory(String targetTopic, FacilityPartitionMapping mapping) {
        this.targetTopic = requireNonNull(targetTopic);
        this.mapping = requireNonNull(mapping);
    }

    @Override
    public ProducerRecord<String, String> producerRecordFor(AirborneEvent record) {

        String key = record.nopFacility().toString(); //examples include "D10", "A80"
        String value = record.asJson();
        int partition = partitionFor(record);

        Header[] headers = extractHeaders(record);

        return new ProducerRecord<>(targetTopic, partition, key, value, newArrayList(headers));
    }

    private int partitionFor(AirborneEvent e) {
        return mapping.partitionFor(e.nopFacility()).get();
    }

    static Header[] extractHeaders(AirborneEvent record) {

        //Kafka Header -VALUES-
        String facility = record.nopFacility().toString();
        String ifrVfrStatus = record.pairedIfrVfrStatus().toString();
        String scoreAsString = Double.toString(record.score());
        String epochTimeMs = Long.toString(record.time().toEpochMilli());

        Headers headers = new RecordHeaders()
            .add("facility", facility.getBytes()) //e.g. D10, A80, or ZFW
            .add("ifrVfrStatus", ifrVfrStatus.getBytes()) //IFR-IFR, IFR-VFR, or VFR-VFR
            .add("date", record.eventDate().getBytes()) //e.g. 2020-03-27, YYYY-MM-DD
            .add("time", record.eventTimeOfDay().getBytes()) //e.g. HH:mm:ss.SSS
            .add("callsign_0", record.callsign(0).getBytes())
            .add("callsign_1", record.callsign(1).getBytes())
            .add("eventScore", scoreAsString.getBytes()) //notice, this double is actually stored as a byte[] that encodes a String
            .add("epochTimeMs", epochTimeMs.getBytes())
            .add("conflictAngle", record.conflictAngle().toString().getBytes()) //e.g. CROSSING, SAME, or OPPOSITE
            .add("schemaVer", record.schemaVersion().getBytes());

        return headers.toArray();
    }

}
