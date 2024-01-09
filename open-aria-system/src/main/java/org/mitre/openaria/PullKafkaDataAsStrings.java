package org.mitre.openaria;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.fileutil.FileUtils.buildGzWriter;
import static org.mitre.caasd.commons.fileutil.FileUtils.getProperties;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


/**
 * PullKafkaDataAsStrings is an executable program that downloads messages from Kafka and writes
 * those messages to a .gz file (or to System.out in some config settings).
 *
 * <p>All Kafka messages are "Strings" because the `value.deserializer` we load through the Kafka
 * Properties file is responsible for converting Kafka Message Values to Strings.
 *
 * <p>PullKafkaDataAsStrings will (A) download messages from a single partition of a Kafka topic
 * (using the -p <PARTITION_NUM> command line flags) or (B) download messages from every partition
 * of a Kafka topic (using the --all command line flag)
 *
 * <p>By default PullKafkaDataAsStrings downloads all messages from any partitions it is configured
 * to pull. If you use the -n <NUM_MESSAGES> command line flag the program will limit the number of
 * messages that are downloaded from Kafka.
 *
 * <p>The Command Line Flags are:
 * (required) -t <TOPIC>
 * (required) -p <PARTITION_NUM>
 * (required) -c <CONFIG_FILE>
 * (optional) -n <NUM_MESSAGES>
 * (optional) --all  (replaces -p when used)
 * (optional) --offsets (adds each message's Kafka offset to the output)
 * (optional) --timestamps (adds each message's Kafka write timestamp to the output)
 * (optional) --watch (this flag lets us watch data roll in, no .gz file are produced)
 *
 * <p>To run this program use:
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t <TOPIC> -p <PARTITION_NUM> -c <CONFIG_FILE>
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t <TOPIC> -p <PARTITION_NUM> -c <CONFIG_FILE> -n <NUM_MESSAGES>
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t <TOPIC> --all -c <CONFIG_FILE> -n <NUM_MESSAGES>
 *
 * <p>For example:
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t nopPoints20_HA -p 5 -c dataPull.props
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t nopPoints20_HA -p 5 -c dataPull.props -n 5000
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t nopPoints20_HA --all -c dataPull.props
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t nopPoints20_HA --all -c dataPull.props -n 5000
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t nopPoints20_HA --all -c dataPull.props -n 5000 --offsets --timestamps
 * java -cp ARIA.jar org.mitre.openaria.PullKafkaDataAsStrings -t nopPoints20_HA --all -c dataPull.props --watch --offsets --timestamps
 *
 * <p>Note: This program replaced PullStringDataFromKafka
 */
public class PullKafkaDataAsStrings {

    /** Use JCommander command line argument parser utility to create this class. */
    static class Args {

        @Parameter(names = {"-t"}, required = true, description = "The kafka topic to pull")
        String topic;

        @Parameter(names = {"-p"}, required = false, description = "The partition to pull")
        Integer partitionNum;

        @Parameter(names = {"-c"}, required = true, description = "The kafka config file")
        String kafkaConfigFile;

        @Parameter(names = {"-n"}, required = false, description = "The number of messages to pull a partition")
        Integer numMessages;

        @Parameter(names = {"--all"}, required = false, description = "Use this flag to Pull data from ALL partitions")
        boolean pullAllPartitions;

        @Parameter(names = {"--offsets"}, required = false, description = "Use this flag to print Kafka offsets")
        boolean showOffsets;

        @Parameter(names = {"--timestamps"}, required = false, description = "Use this flag to print Kafka upload timestamps")
        boolean showTimeStamps;

        @Parameter(names = {"--watch"}, required = false, description = "Use this flag to watch, not write to file")
        boolean justWatching;

        //manually verify parameter values using this method
        void verifyArgs() {
            checkArgument(!(pullAllPartitions && nonNull(partitionNum)), "Cannot use -p AND --all at the same time");
            checkArgument(pullAllPartitions || nonNull(partitionNum), "Must use -p OR --all");
            checkArgument(!(justWatching && nonNull(numMessages)), "Cannot use --watch and -n at the same time");

            if (nonNull(numMessages)) {
                checkArgument(numMessages > 0, "Must pull at least 1 message");
            }
        }

        boolean limitingMessageCount() {
            return nonNull(numMessages);
        }

        int maxMessageCount() {
            return limitingMessageCount() ? numMessages : Integer.MAX_VALUE;
        }

        Properties kafkaProperties() {
            try {
                return getProperties(new File(kafkaConfigFile));
            } catch (Exception ex) {
                throw demote(ex);
            }
        }

        /** Get a RecordConverter that bundles the "showOffsets" and "showTimeStamps" flags. */
        RecordConverter recordConverter() {
            return new RecordConverter(showOffsets, showTimeStamps);
        }

        /**
         * Based on the user supplied command line arguments generate a list of TopicPartitions that
         * should be pulled.
         */
        List<TopicPartition> partitionsToUse() {
            return (pullAllPartitions)
                ? listOfTopicPartitions(kafkaProperties(), topic)
                : newArrayList(new TopicPartition(topic, partitionNum));
        }
    }

    /* Use JCommander util to parse the command line args */
    static Args parseCommandLineArgs(String[] args) {

        Args parsedArgs = new Args();
        JCommander.newBuilder()
            .addObject(parsedArgs)
            .build()
            .parse(args);

        parsedArgs.verifyArgs();

        return parsedArgs;
    }


    public static void main(String[] argv) throws Exception {

        Args args = parseCommandLineArgs(argv);

        //If you just want to watch data roll into Kafka, set up an InfiniteKafkaWatcher
        if (args.justWatching) {
            InfiniteKafkaWatcher watcher = new InfiniteKafkaWatcher(
                args.kafkaProperties(),
                args.partitionsToUse(),
                args.recordConverter()
            );

            watcher.watch();  //begins an infinite loop !!
        }

        //If you want to drain data from Kafka, list all the partitions you want, then pull them one by one
        if (!args.justWatching) {

            List<TopicPartition> partitionsToDrain = args.partitionsToUse();

            partitionsToDrain.forEach(
                topicPartition -> drainOnePartition(args, topicPartition)
            );
        }
    }

    /** The one TopicPartition that will be drained. */
    private final TopicPartition topicPartition;

    /** This KafkaConsumer is configured to retrieve data from exactly one partition. */
    private final KafkaConsumer<String, String> kafkaConsumer;

    //Save offset where you will stop extracting data (because data may be arriving continuously)
    private final Map<TopicPartition, Long> stoppingOffset;

    private final RecordConverter toStringRule;

    private final int maxNumMessages;

    /**
     * This object knows how to "drain a TopicPartition"
     *
     * @param args The command line parameters (the args that govern partitions are NOT used)
     * @param tp   The topic partition to drain
     */
    private PullKafkaDataAsStrings(Args args, TopicPartition tp) {
        checkNotNull(args);
        checkNotNull(tp);
        checkNotNull(args.kafkaConfigFile, "The properties file cannot be null");

        printSetupMessage(newArrayList(tp));

        this.topicPartition = tp;

        //Set up a KafkaConsumer to drain from the beginning of a topic...
        this.kafkaConsumer = new KafkaConsumer<>(args.kafkaProperties());
        kafkaConsumer.assign(newArrayList(tp));
        kafkaConsumer.seekToBeginning(newArrayList(tp));

        this.stoppingOffset = kafkaConsumer.endOffsets(kafkaConsumer.assignment());
        this.maxNumMessages = args.maxMessageCount();
        this.toStringRule = args.recordConverter();
    }

    private void drainPartitionToFile() {

        PrintWriter msgGzStream = gzWriterFor(topicPartition);
        Consumer<String> messageSink = (String msg) -> msgGzStream.write(msg + "\n");

        pullPointData(messageSink);

        msgGzStream.close();
        kafkaConsumer.close();
    }


    /** Pull at most n Kafka Messages and write the data to a file. */
    private void pullPointData(Consumer<String> messageSink) {

        /*
         * poll kafka for point data and write all data to the .gz file until there is no more new
         * data to get
         */
        int totalPointsDownloaded = 0;
        while (true) {
            int round1 = getKafkaDataAsStrings(messageSink);
            int round2 = getKafkaDataAsStrings(messageSink);

            totalPointsDownloaded += round1;
            totalPointsDownloaded += round2;

            if (round1 + round2 == 0) {
                break;
            }

            if (totalPointsDownloaded > maxNumMessages) {
                break;
            }
        }
    }

    private int getKafkaDataAsStrings(Consumer<String> messageSink) {

        Duration timeout = Duration.ofMillis(8_000);

        System.out.println("polling..");
        ConsumerRecords<String, String> records = kafkaConsumer.poll(timeout);
        System.out.println("done polling [" + records.count() + " records retrieved]");

        int count = 0;

        for (ConsumerRecord<String, String> record : records) {

            TopicPartition tp = new TopicPartition(record.topic(), record.partition());
            if (record.offset() < stoppingOffset.get(tp)) {
                //include record in output
                count++;
                String decoratedMessage = toStringRule.apply(record);

                messageSink.accept(decoratedMessage);

            } else {
                //ignore record
            }
        }

        return count;
    }


    /**
     * Drain a Kafka TopicPartition to a file
     *
     * @param args The parsed command line args (not all args are used)
     * @param tp   The topic partition to drain
     */
    private static void drainOnePartition(Args args, TopicPartition tp) {
        PullKafkaDataAsStrings dataPuller = new PullKafkaDataAsStrings(args, tp);
        dataPuller.drainPartitionToFile();
    }

    /** A Watcher is a "Helper" class that wraps all Kafka interaction. */
    static class InfiniteKafkaWatcher {

        private final KafkaConsumer<String, String> kafkaConsumer;

        private final RecordConverter toStringRule;

        InfiniteKafkaWatcher(Properties kafkaProps, List<TopicPartition> partitionsToPull, RecordConverter toStringRule) {
            requireNonNull(kafkaProps);
            requireNonNull(partitionsToPull);
            requireNonNull(toStringRule);

            printSetupMessage(partitionsToPull);

            this.kafkaConsumer = new KafkaConsumer<>(kafkaProps);
            kafkaConsumer.assign(partitionsToPull);  //using "assign" in isolate starts at beginning of topic
            kafkaConsumer.seekToEnd(partitionsToPull);

            this.toStringRule = toStringRule;
        }

        public void watch() {

            Consumer<String> messageSink = (String msg) -> System.out.println(msg);

            while (true) {
                pollKafkaAndProcessMessages(messageSink);
            }
        }

        private void pollKafkaAndProcessMessages(Consumer<String> messageSink) {

            Duration timeout = Duration.ofMillis(8_000);

            System.out.println("polling..");
            ConsumerRecords<String, String> records = kafkaConsumer.poll(timeout);
            System.out.println("done polling [" + records.count() + " records retrieved]");

            for (ConsumerRecord<String, String> record : records) {
                String decoratedMessage = toStringRule.apply(record);
                messageSink.accept(decoratedMessage);
            }
        }
    }

    /**
     * Build a KafkaConsumer, extract metadata about how many partitions are available for a
     * particular topic.  Return a list of all these TopicPartitions.
     */
    private static List<TopicPartition> listOfTopicPartitions(Properties kafkaProps, String topic) {

        KafkaConsumer<String, String> kc = new KafkaConsumer<>(kafkaProps);
        List<PartitionInfo> infos = kc.partitionsFor(topic);

        kc.close();

        return infos.stream()
            .map(info -> new TopicPartition(info.topic(), info.partition()))
            .collect(Collectors.toList());
    }

    /** Converts a Kafka ConsumerRecord to a String, adds Offset and Timestamp decorations too. */
    static class RecordConverter implements Function<ConsumerRecord<String, String>, String> {

        private final boolean showOffsets;

        private final boolean showTimestamps;

        RecordConverter(boolean showOffsets, boolean showTimestamps) {
            this.showOffsets = showOffsets;
            this.showTimestamps = showTimestamps;
        }

        @Override
        public String apply(ConsumerRecord<String, String> record) {

            StringBuilder sb = new StringBuilder();

            if (this.showOffsets) {
                sb.append(record.offset() + "\t");
            }

            if (this.showTimestamps) {
                Instant kafkaUploadTime = Instant.ofEpochMilli(record.timestamp());
                sb.append(kafkaUploadTime.toString() + "\t");
            }

            String messageAsStr = record.value();

            return sb.append(messageAsStr).toString();
        }
    }

    /** @return A PrintWriter that sends data to a file named: "dataFrom_TOPIC_PARTITION.txt.gz". */
    private static PrintWriter gzWriterFor(TopicPartition tp) {
        return gzWriterFor(tp.topic(), tp.partition());
    }

    /** @return A PrintWriter that sends data to a file named: "dataFrom_TOPIC_PARTITION.txt.gz". */
    private static PrintWriter gzWriterFor(String topic, int partition) {

        try {
            return buildGzWriter(new File("dataFrom_" + topic + "_" + partition + ".txt.gz"));
        } catch (IOException e) {
            throw demote(e);
        }
    }

    static void printSetupMessage(List<TopicPartition> partitionsToPull) {

        StringBuilder sb = new StringBuilder("Retrieving data from:");
        partitionsToPull.forEach(
            tp -> sb.append("\n  " + tp.toString())
        );
        System.out.println(sb.toString());
    }

}