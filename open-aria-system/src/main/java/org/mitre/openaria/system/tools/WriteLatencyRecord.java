package org.mitre.openaria.system.tools;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.out.JsonWritable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * A WriteLatencyRecord keeps track of how much time passed between (1) an Radar Hit occurring and
 * (2) its corresponding message being written to Kafka.
 */
public class WriteLatencyRecord implements JsonWritable {

    /* The converter is static to allow reuse. Creating the Gson using reflection is expensive. */
    private static final Gson GSON_CONVERTER = new GsonBuilder().setPrettyPrinting().create();

    /** Date defined like: yyyy-mm-dd */
    private String date_of_event;

    private int kafka_partition;

    private int event_to_write_latency_under_30_sec;
    private int event_to_write_latency_under_1_min;
    private int event_to_write_latency_under_5_min;
    private int event_to_write_latency_under_15_min;
    private int event_to_write_latency_under_30_min;
    private int event_to_write_latency_under_1_hour;
    private int event_to_write_latency_under_4_hour;
    private int event_to_write_latency_under_12_hour;
    private int event_to_write_latency_under_1_day;
    private int event_to_write_latency_under_2_day;
    private int event_to_write_latency_over_2_day;

    /** The number of points there were ingest on this day for this Facility. */
    private long point_count;

    /**
     * This constructor creates an Aggregate Record that summarizes data across multiple
     * partitions.
     */
    WriteLatencyRecord(Collection<WriteLatencyRecord> combineAll) {

        long numDates = combineAll.stream()
            .map(record -> record.date_of_event)
            .distinct()
            .count();

        checkArgument(numDates == 1, "Can only combine data from exactly 1 date");
        this.date_of_event = combineAll.stream().map(record -> record.date_of_event).findFirst().get();
        this.kafka_partition = -1;

        this.event_to_write_latency_under_30_sec = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_30_sec).sum();
        this.event_to_write_latency_under_1_min = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_1_min).sum();
        this.event_to_write_latency_under_5_min = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_5_min).sum();
        this.event_to_write_latency_under_15_min = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_15_min).sum();
        this.event_to_write_latency_under_30_min = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_30_min).sum();
        this.event_to_write_latency_under_1_hour = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_1_hour).sum();
        this.event_to_write_latency_under_4_hour = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_4_hour).sum();
        this.event_to_write_latency_under_12_hour = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_12_hour).sum();
        this.event_to_write_latency_under_1_day = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_1_day).sum();
        this.event_to_write_latency_under_2_day = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_under_2_day).sum();
        this.event_to_write_latency_over_2_day = combineAll.stream()
            .mapToInt(record -> record.event_to_write_latency_over_2_day).sum();

        this.point_count = combineAll.stream().mapToLong(record -> record.point_count).sum();
    }

    public WriteLatencyRecord(String radarHitDate, ConsumerRecord<?, ?> firstRecord, Point point) {

        this.date_of_event = radarHitDate;
        this.kafka_partition = firstRecord.partition();
        this.event_to_write_latency_under_30_sec = 0;
        this.event_to_write_latency_under_1_min = 0;
        this.event_to_write_latency_under_5_min = 0;
        this.event_to_write_latency_under_15_min = 0;
        this.event_to_write_latency_under_30_min = 0;
        this.event_to_write_latency_under_1_hour = 0;
        this.event_to_write_latency_under_4_hour = 0;
        this.event_to_write_latency_under_12_hour = 0;
        this.event_to_write_latency_under_1_day = 0;
        this.event_to_write_latency_under_2_day = 0;
        this.event_to_write_latency_over_2_day = 0;
        this.point_count = 1;

        //compute latency between Point.time() and firstRecord.timeStamp

        incrementLatencyCounts(
            computeLatencyInSec(firstRecord, point)
        );

        //IDEALLY, WE WOULD KEEP TRACK OF SOME SORT OF LATENCY HERE
        //DELTA BETWEEN KAFKA WRITE TIME AND KAFKA PROCESS TIME
        //DELTA BETWEEN POINT TIME AND KAFKA WRITE TIME..
    }

    public WriteLatencyRecord(WriteLatencyRecord prior, ConsumerRecord<?, ?> newRecord, Point point) {
        this.date_of_event = prior.date_of_event;
        this.kafka_partition = newRecord.partition();
        checkArgument(
            this.kafka_partition == prior.kafka_partition,
            "Kafka partition mismatch: " + this.kafka_partition + " vs. " + prior.kafka_partition
        );

        this.event_to_write_latency_under_30_sec = prior.event_to_write_latency_under_30_sec;
        this.event_to_write_latency_under_1_min = prior.event_to_write_latency_under_1_min;
        this.event_to_write_latency_under_5_min = prior.event_to_write_latency_under_5_min;
        this.event_to_write_latency_under_15_min = prior.event_to_write_latency_under_15_min;
        this.event_to_write_latency_under_30_min = prior.event_to_write_latency_under_30_min;
        this.event_to_write_latency_under_1_hour = prior.event_to_write_latency_under_1_hour;
        this.event_to_write_latency_under_4_hour = prior.event_to_write_latency_under_4_hour;
        this.event_to_write_latency_under_12_hour = prior.event_to_write_latency_under_12_hour;
        this.event_to_write_latency_under_1_day = prior.event_to_write_latency_under_1_day;
        this.event_to_write_latency_under_2_day = prior.event_to_write_latency_under_2_day;
        this.event_to_write_latency_over_2_day = prior.event_to_write_latency_over_2_day;
        this.point_count = prior.point_count + 1;
        incrementLatencyCounts(
            computeLatencyInSec(newRecord, point)
        );
    }

    private long computeLatencyInSec(ConsumerRecord<?, ?> kafkaRecord, Point point) {
        //write to Ingest latency...
        long eventOccuranceTime = point.time().toEpochMilli();
        long kafkaWriteTime = kafkaRecord.timestamp();
        long latencyMs = kafkaWriteTime - eventOccuranceTime;
        long latencySec = latencyMs / 1000;

        return latencySec;
    }

    @Override
    public String asJson() {
        //use the static Gson to prevent frequent use of expensive java.lang.reflect calls
        return GSON_CONVERTER.toJson(this);
    }

    public static ReadLatencyRecord parseJson(String json) {
        ReadLatencyRecord objGeneratedFromJson = GSON_CONVERTER.fromJson(json, ReadLatencyRecord.class);
        return objGeneratedFromJson;
    }

    private void incrementLatencyCounts(long latencyInSec) {
        if (latencyInSec < 30) {
            event_to_write_latency_under_30_sec++;
            return;
        }
        if (latencyInSec < 60) {
            event_to_write_latency_under_1_min++;
            return;
        }
        if (latencyInSec < 60 * 5) {
            event_to_write_latency_under_5_min++;
            return;
        }
        if (latencyInSec < 60 * 15) {
            event_to_write_latency_under_15_min++;
            return;
        }
        if (latencyInSec < 60 * 30) {
            event_to_write_latency_under_30_min++;
            return;
        }

        if (latencyInSec < 60 * 60) {
            event_to_write_latency_under_1_hour++;
            return;
        }

        if (latencyInSec < 60 * 60 * 4) {
            event_to_write_latency_under_4_hour++;
            return;
        }

        if (latencyInSec < 60 * 60 * 12) {
            event_to_write_latency_under_12_hour++;
            return;
        }

        if (latencyInSec < 60 * 60 * 24) {
            event_to_write_latency_under_1_day++;
            return;
        }

        if (latencyInSec < 60 * 60 * 48) {
            event_to_write_latency_under_2_day++;
            return;
        }

        event_to_write_latency_over_2_day++;
    }
}
