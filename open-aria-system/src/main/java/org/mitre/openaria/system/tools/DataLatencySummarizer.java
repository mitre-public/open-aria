package org.mitre.openaria.system.tools;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static org.mitre.caasd.commons.util.DemotedException.demote;
import static org.mitre.openaria.core.utils.TimeUtils.utcDateAsString;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.openaria.core.Point;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * A DataLatencySummarizer measures the latency of data pulled from Kafka.
 * <p>
 * The goal is to generate log that provide insight into (1) the amount of time between when
 * real-world event occur and when those events are recorded in Kafka and (2) the amount of time
 * that passes between an event being written to Kafka and that data being processed by a Streaming
 * consumer.
 */
public class DataLatencySummarizer {

    /** How long to retain records (i.e. table rows with latency data) */
    private static final int NUM_DAYS_KEPT = 14;

    //contains summaries of data pulled on a specific Date (i.e. "2020-01-13"") and facility (e.g. "A80" or "KATL")
    Table<String, String, ReadLatencyRecord> readLatencySummaries;

    /** Contains summaries of lag between Radar Hit times and Kafka Write times. */
    Table<String, String, WriteLatencyRecord> writeLatencySummaries;

    DataLatencySummarizer() {
        this.readLatencySummaries = HashBasedTable.create();
        this.writeLatencySummaries = HashBasedTable.create();
    }

    /**
     * @return a {@link DataLatencySummarizer} that summarizes latency by Kafka partition number
     */
    public static DataLatencySummarizer byPartitionSummarizer() {
        return new DataLatencySummarizer();
    }

    public void incorporate(ConsumerRecord<?, ?> kafkaRecord, Point point) {

        Instant now = Instant.now();

        int partition = kafkaRecord.partition();
        String partitionAsString = Integer.toString(partition);

        String consumptionDate = utcDateAsString(now);

        updateReadLatency(kafkaRecord, now, consumptionDate, partitionAsString);
        updateWriteLatency(kafkaRecord, partitionAsString, point);
    }

    private void updateReadLatency(ConsumerRecord<?, ?> kafkaRecord, Instant now, String consumptionDate, String partitionNum) {
        //message creation to message consumption latency
        long latencySec = computeLatencyInSec(kafkaRecord, now);

        ReadLatencyRecord prior = readLatencySummaries.get(consumptionDate, partitionNum);
        if (prior == null) {
            readLatencySummaries.put(consumptionDate, partitionNum, new ReadLatencyRecord(consumptionDate, kafkaRecord, latencySec));
        } else {
            readLatencySummaries.put(consumptionDate, partitionNum, new ReadLatencyRecord(prior, kafkaRecord, latencySec));
        }
    }

    private void updateWriteLatency(ConsumerRecord<?, ?> kafkaRecord, String partitionNum, Point point) {

        String eventDate = utcDateAsString(point.time());

        //radar hit occurrance to message creation latency
        WriteLatencyRecord prior = writeLatencySummaries.get(eventDate, partitionNum);
        if (prior == null) {
            writeLatencySummaries.put(eventDate, partitionNum, new WriteLatencyRecord(eventDate, kafkaRecord, point));
        } else {
            writeLatencySummaries.put(eventDate, partitionNum, new WriteLatencyRecord(prior, kafkaRecord, point));
        }
    }

    private long computeLatencyInSec(ConsumerRecord<?, ?> kafkaRecord, Instant now) {
        //write to Ingest latency...
        long kafkaWriteTime = kafkaRecord.timestamp();
        long pullFromKafkaTime = now.toEpochMilli();
        long latencyMs = pullFromKafkaTime - kafkaWriteTime;
        long latencySec = latencyMs / 1000;

        return latencySec;
    }

    //this method really should be somewhere else...but for now it goes here.
    public void writeLogs(String logDirectory) {

        //write the aggregate stats to one file...
        FileUtils.makeDirIfMissing(logDirectory);

        for (String date : readLatencySummaries.rowKeySet()) {

            String fileName = logDirectory + File.separator + "dataReadLatencyFor-" + date + ".txt";
            try (FileWriter fw = new FileWriter(new File(fileName), false)) {
                fw.append(summarizeReadLatencyDate(date));
                fw.flush();
            } catch (IOException ioe) {
                throw demote(ioe);
            }
        }

        for (String date : writeLatencySummaries.rowKeySet()) {

            String fileName = logDirectory + File.separator + "dataCreationLatencyFor-" + date + ".txt";
            try (FileWriter fw = new FileWriter(new File(fileName), false)) {
                fw.append(summarizeWriteLatencyData(date));
                fw.flush();
            } catch (IOException ioe) {
                throw demote(ioe);
            }
        }

        removeOldRecords();
    }

    private String summarizeReadLatencyDate(String dateKey) {

        StringBuilder sb = new StringBuilder();

        Map<String, ReadLatencyRecord> facilityMap = this.readLatencySummaries.row(dateKey);

        for (String partitionNumber : readLatencySummaries.columnKeySet()) {
            ReadLatencyRecord record = facilityMap.get(partitionNumber);

            if (record == null) {
                sb.append("\n" + partitionNumber + " -- EMPTY");
            } else {
                sb.append("\n" + partitionNumber + " ");
                sb.append(record.asJson());
            }
        }

        ReadLatencyRecord combo = new ReadLatencyRecord(readLatencySummaries.row(dateKey).values());
        sb.append("\nIn Total: ").append(combo.asJson()).append("\n");

        return sb.toString();
    }

    private String summarizeWriteLatencyData(String dateKey) {
        StringBuilder sb = new StringBuilder();

        Map<String, WriteLatencyRecord> facilityMap = this.writeLatencySummaries.row(dateKey);

        for (String partitionNumber : writeLatencySummaries.columnKeySet()) {
            WriteLatencyRecord record = facilityMap.get(partitionNumber);

            if (record == null) {
                sb.append("\n" + partitionNumber + " -- EMPTY");
            } else {
                sb.append("\n" + partitionNumber + " ");
                sb.append(record.asJson());
            }
        }

        WriteLatencyRecord combo = new WriteLatencyRecord(writeLatencySummaries.row(dateKey).values());
        sb.append("\nIn Total: ").append(combo.asJson()).append("\n");

        return sb.toString();
    }

    private void removeOldRecords() {
        removeOldRecords(readLatencySummaries);
        removeOldRecords(writeLatencySummaries);
    }

    private void removeOldRecords(Table<String, String, ?> table) {

        LinkedList<String> days = newLinkedList(table.rowKeySet());
        Collections.sort(days);

        while (days.size() > NUM_DAYS_KEPT) {
            String day = days.removeFirst(); //remove oldest day..

            //build a new list to avoid Concurrent Modification Exceptions
            List<String> removeThese = newArrayList(table.row(day).keySet());

            for (String partitionNum : removeThese) {
                table.remove(day, partitionNum);
            }
        }
    }
}
