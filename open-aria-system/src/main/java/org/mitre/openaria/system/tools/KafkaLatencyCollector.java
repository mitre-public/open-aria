package org.mitre.openaria.system.tools;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.mitre.openaria.core.Point;

/**
 * A KafkaLatencyCollector aggregates the latency associated with Kafka's Point Messages. It keeps
 * track of (1) how quickly Point data arrives in Kafka and (2) how quickly Point data is downloaded
 * from Kafka for processing.
 */
public class KafkaLatencyCollector {

    long pointCount;

    long totalPointConsumeLatencyMilliSec;

    long totalPointUploadLatencyMilliSec;

    public KafkaLatencyCollector() {
        this.pointCount = 0;
        this.totalPointConsumeLatencyMilliSec = 0;
        this.totalPointUploadLatencyMilliSec = 0;
    }

    public void incorporate(ConsumerRecord<?, ?> kafkaRecord, Point point) {
        pointCount++;
        //use += when query time comes after "write to Kafka" step
        totalPointConsumeLatencyMilliSec += computeLatencyInMilliSec(kafkaRecord, Instant.now());
        //use -= when query time comes before "write to Kafka" step
        totalPointUploadLatencyMilliSec -= computeLatencyInMilliSec(kafkaRecord, point.time());
    }

    /**
     * Compute the Time between (a) when a piece of data got written Kafka and (b) the other time
     *
     * @param kafkaRecord A Record extracted from Kafka what will have a timestamp embedded in it.
     * @param otherTime   Another time value
     *
     * @return the time (millis) between when the kafkaRecord was written to Kafka and otherTime
     */
    private long computeLatencyInMilliSec(ConsumerRecord<?, ?> kafkaRecord, Instant otherTime) {
        long writtenToKafkaTime = kafkaRecord.timestamp();
        long latencyMs = otherTime.toEpochMilli() - writtenToKafkaTime;

        return latencyMs;
    }

    public long curPointCount() {
        return pointCount;
    }

    public long totalConsumeLatencyMilliSec() {
        return totalPointConsumeLatencyMilliSec;
    }

    public long totalUploadLatencyMilliSec() {
        return totalPointUploadLatencyMilliSec;
    }
}
