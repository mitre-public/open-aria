package org.mitre.openaria.system.tools;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;

import org.mitre.caasd.commons.out.JsonWritable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * A ReadLatencyRecord keeps track of how much time passed between (1) a message being written to
 * Kafka and (2) that message being retrieved for processing.
 */
public class ReadLatencyRecord implements JsonWritable {

    /* The converter is static to allow reuse. Creating the Gson using reflection is expensive. */
    private static final Gson GSON_CONVERTER = new GsonBuilder().setPrettyPrinting().create();

    /** Date defined like: yyyy-mm-dd */
    private String date_of_data_consumption;

    private int kafka_partition;

    private int write_to_read_latency_under_30_sec;
    private int write_to_read_latency_under_1_min;
    private int write_to_read_latency_under_5_min;
    private int write_to_read_latency_under_15_min;
    private int write_to_read_latency_under_30_min;
    private int write_to_read_latency_under_1_hour;
    private int write_to_read_latency_under_4_hour;
    private int write_to_read_latency_under_12_hour;
    private int write_to_read_latency_under_1_day;
    private int write_to_read_latency_under_2_day;
    private int write_to_read_latency_over_2_day;

    /** The number of points there were ingest on this day for this Facility. */
    private long point_count;

    /**
     * This constructor creates an Aggregate Record that summarizes data across multiple
     * partitions.
     */
    ReadLatencyRecord(Collection<ReadLatencyRecord> combineAll) {

        long numDates = combineAll.stream()
            .map(record -> record.date_of_data_consumption)
            .distinct()
            .count();

        checkArgument(numDates == 1, "Can only combine data from exactly 1 date");
        this.date_of_data_consumption = combineAll.stream().map(record -> record.date_of_data_consumption).findFirst().get();
        this.kafka_partition = -1;

        this.write_to_read_latency_under_30_sec = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_30_sec).sum();
        this.write_to_read_latency_under_1_min = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_1_min).sum();
        this.write_to_read_latency_under_5_min = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_5_min).sum();
        this.write_to_read_latency_under_15_min = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_15_min).sum();
        this.write_to_read_latency_under_30_min = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_30_min).sum();
        this.write_to_read_latency_under_1_hour = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_1_hour).sum();
        this.write_to_read_latency_under_4_hour = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_4_hour).sum();
        this.write_to_read_latency_under_12_hour = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_12_hour).sum();
        this.write_to_read_latency_under_1_day = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_1_day).sum();
        this.write_to_read_latency_under_2_day = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_under_2_day).sum();
        this.write_to_read_latency_over_2_day = combineAll.stream()
            .mapToInt(record -> record.write_to_read_latency_over_2_day).sum();

        this.point_count = combineAll.stream().mapToLong(record -> record.point_count).sum();
    }

    public ReadLatencyRecord(String ingestDate, ConsumerRecord<?, ?> firstRecord, long latencyInSec) {

        this.date_of_data_consumption = ingestDate;
        this.kafka_partition = firstRecord.partition();
        this.write_to_read_latency_under_30_sec = 0;
        this.write_to_read_latency_under_1_min = 0;
        this.write_to_read_latency_under_5_min = 0;
        this.write_to_read_latency_under_15_min = 0;
        this.write_to_read_latency_under_30_min = 0;
        this.write_to_read_latency_under_1_hour = 0;
        this.write_to_read_latency_under_4_hour = 0;
        this.write_to_read_latency_under_12_hour = 0;
        this.write_to_read_latency_under_1_day = 0;
        this.write_to_read_latency_under_2_day = 0;
        this.write_to_read_latency_over_2_day = 0;
        this.point_count = 1;

        incrementLatencyCounts(latencyInSec);

        //IDEALLY, WE WOULD KEEP TRACK OF SOME SORT OF LATENCY HERE
        //DELTA BETWEEN KAFKA WRITE TIME AND KAFKA PROCESS TIME
        //DELTA BETWEEN POINT TIME AND KAFKA WRITE TIME...
    }

    public ReadLatencyRecord(ReadLatencyRecord prior, ConsumerRecord<?, ?> newRecord, long latencyInSec) {
        this.date_of_data_consumption = prior.date_of_data_consumption;
        this.kafka_partition = newRecord.partition();
        checkArgument(
            this.kafka_partition == prior.kafka_partition,
            "Kafka partition mismatch: " + this.kafka_partition + " vs. " + prior.kafka_partition
        );

        this.write_to_read_latency_under_30_sec = prior.write_to_read_latency_under_30_sec;
        this.write_to_read_latency_under_1_min = prior.write_to_read_latency_under_1_min;
        this.write_to_read_latency_under_5_min = prior.write_to_read_latency_under_5_min;
        this.write_to_read_latency_under_15_min = prior.write_to_read_latency_under_15_min;
        this.write_to_read_latency_under_30_min = prior.write_to_read_latency_under_30_min;
        this.write_to_read_latency_under_1_hour = prior.write_to_read_latency_under_1_hour;
        this.write_to_read_latency_under_4_hour = prior.write_to_read_latency_under_4_hour;
        this.write_to_read_latency_under_12_hour = prior.write_to_read_latency_under_12_hour;
        this.write_to_read_latency_under_1_day = prior.write_to_read_latency_under_1_day;
        this.write_to_read_latency_under_2_day = prior.write_to_read_latency_under_2_day;
        this.write_to_read_latency_over_2_day = prior.write_to_read_latency_over_2_day;
        this.point_count = prior.point_count + 1;
        incrementLatencyCounts(latencyInSec);
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
            write_to_read_latency_under_30_sec++;
            return;
        }
        if (latencyInSec < 60) {
            write_to_read_latency_under_1_min++;
            return;
        }
        if (latencyInSec < 60 * 5) {
            write_to_read_latency_under_5_min++;
            return;
        }
        if (latencyInSec < 60 * 15) {
            write_to_read_latency_under_15_min++;
            return;
        }
        if (latencyInSec < 60 * 30) {
            write_to_read_latency_under_30_min++;
            return;
        }

        if (latencyInSec < 60 * 60) {
            write_to_read_latency_under_1_hour++;
            return;
        }

        if (latencyInSec < 60 * 60 * 4) {
            write_to_read_latency_under_4_hour++;
            return;
        }

        if (latencyInSec < 60 * 60 * 12) {
            write_to_read_latency_under_12_hour++;
            return;
        }

        if (latencyInSec < 60 * 60 * 24) {
            write_to_read_latency_under_1_day++;
            return;
        }

        if (latencyInSec < 60 * 60 * 48) {
            write_to_read_latency_under_2_day++;
            return;
        }

        write_to_read_latency_over_2_day++;
    }
}
