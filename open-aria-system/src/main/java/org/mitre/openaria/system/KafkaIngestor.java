

package org.mitre.openaria.system;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.requireNonNull;
import static org.apache.kafka.clients.consumer.ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG;
import static org.mitre.openaria.core.NopPoint.parseSafely;
import static org.mitre.openaria.kafka.KafkaPropertyUtils.verifyKafkaBrokers;
import static org.mitre.caasd.commons.parsing.nop.Facility.toFacility;
import static org.mitre.caasd.commons.parsing.nop.NopMessageType.isNopRadarHit;
import static org.mitre.caasd.commons.util.PropertyUtils.getInt;
import static org.mitre.caasd.commons.util.PropertyUtils.getOptionalBoolean;
import static org.mitre.caasd.commons.util.PropertyUtils.getOptionalInt;
import static org.mitre.caasd.commons.util.PropertyUtils.getString;

import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.mitre.openaria.core.NopPoint;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.kafka.PartitionMapping;
import org.mitre.openaria.system.tools.DataLatencySummarizer;
import org.mitre.openaria.system.tools.KafkaLatencyCollector;
import org.mitre.caasd.commons.parsing.nop.Facility;
import org.mitre.caasd.commons.util.ErrorCatchingTask;
import org.mitre.caasd.commons.util.ExceptionHandler;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A KafkaIngestor pulls and processes data from multiple Kafka Partitions at once. Each Kafka
 * Partition is processed in a separate "swim lane".
 *
 * @param <KAFKA_VAL> The Kafka Value type
 * @param <PK>        The Partition Key (for example NOP Facilities or AsdexAirports). The universe
 *                    of possible partition keys is provided by the PartitionMapping
 */
public class KafkaIngestor<KAFKA_VAL, PK> {

    private final KpiFactory<PK> kpiFactory;

    /** Each Kafka Partition has a dedicated data swim lane. */
    private final ConcurrentHashMap<PK, SwimLane> swimLanes;

    /**
     * This KafkaConsumer is configured to retrieve data for multiple Facilities at once. Retrieving
     * Point data in "larger requests" (that polls data for multiple facilities) is more efficient
     * than making many "smaller requests". (Within reason! Kafka will timeout when data requests
     * get too big to deliver quickly)
     */
    private final KafkaConsumer<String, KAFKA_VAL> kafkaConsumer;

    private final PartitionMapping<PK> partitionMapping;

    private final RecordHelper<KAFKA_VAL, PK> converter;

    /** Collects the data latency measurements that will be periodically written to log files. */
    private final DataLatencySummarizer ingestSummarizer;

    /** These track how up-to-date the raw data we pull from Kafka is. */
    private final Map<PK, KafkaLatencyCollector> latencyCollectors;

    private final AriaServiceAssetBundle serviceAssets;

    /** This object (which is usually built by parsing a .yaml file) configures that algorithm. */
    private final Options options;

    /**
     * @param options          These options control how data is retrieved and processed
     * @param kafkaProps       The Properties passed to a KafkaConsumer constructor
     * @param kpiFactory       This factory makes one StreamingKpi for each Kafka Partition
     * @param partitionMapping Maps Facilities to Partition numbers
     * @param converter        Converts Kafka ConsumerRecords to Points that are route to SwimLanes
     * @param ingestSummarizer Collects data latency measurements that can be periodically written
     *                         to log files
     * @param exceptionHandler The shared exception handling behavior used whenever a Exception
     *                         arises within one of the repeatable tasks.
     */
    public KafkaIngestor(
        Options options,
        Properties kafkaProps,
        KpiFactory<PK> kpiFactory,
        PartitionMapping<PK> partitionMapping,
        RecordHelper<KAFKA_VAL, PK> converter,
        DataLatencySummarizer ingestSummarizer,
        ExceptionHandler exceptionHandler
    ) {
        this.options = requireNonNull(options);
        this.kpiFactory = requireNonNull(kpiFactory);
        this.partitionMapping = requireNonNull(partitionMapping);
        this.swimLanes = buildSwimLanes();

        this.ingestSummarizer = requireNonNull(ingestSummarizer);
        requireNonNull(exceptionHandler);

        this.kafkaConsumer = buildKafkaConsumer(kafkaProps);
        this.converter = requireNonNull(converter);

        this.serviceAssets = new AriaServiceAssetBundle(
            options.numWorkerThreads, exceptionHandler
        );
        this.latencyCollectors = buildLatencyCollectors();

        scheduleDataProcessingTasks();
        scheduleDataPullingTask();
        scheduleIngestSummaryLogging();
        subscribeOrAssignTopics();
    }


    private ConcurrentHashMap<PK, SwimLane> buildSwimLanes() {

        ConcurrentHashMap<PK, SwimLane> map = new ConcurrentHashMap<>();

        for (PK place : partitionMapping.partitionList()) {
            /*
             * The "times 2" implicitly assumes a single pull from Kafka never retrieves more
             * points than the pointPrefetchLimit. For example, assume this swim lane contain 99% of
             * the pointPrefetchLimit. In this case,  we don't want a single pull from Kafka to
             * contain more than 101% of the pointPrefetchLimit because then the queue for this swim
             * lane may not fit all the data that was just downloaded from Kafka
             *
             * A data backup in this queue can, in principle, contain 100% of all prefetch-able data
             * and thus prevent downloading more data from Kafka.
             */
            map.put(place, new SwimLane(kpiFactory.createKpi(place), options.pointPrefetchLimit * 2));
        }
        return map;
    }

    private Map<PK, KafkaLatencyCollector> buildLatencyCollectors() {

        ConcurrentHashMap<PK, KafkaLatencyCollector> map = new ConcurrentHashMap<>();

        for (PK place : partitionMapping.partitionList()) {
            map.put(place, new KafkaLatencyCollector());
        }
        return map;
    }

    /* Perform some light validation of the Properties object we use to configure Kafka. */
    private KafkaConsumer<String, KAFKA_VAL> buildKafkaConsumer(Properties kafkaProps) {
        verifyKafkaBrokers(kafkaProps);
        checkState(
            kafkaProps.containsKey(PARTITION_ASSIGNMENT_STRATEGY_CONFIG),
            "The Kafka Properties config must contain a partition.assignment.strategy"
        );

        return new KafkaConsumer<>(kafkaProps);
    }

    /** Here is where we tell our KafkaConsumer which topics to get data from. */
    private void subscribeOrAssignTopics() {

        if (options.useConsumerGroups) {
            /*
             * This is the "standard" approach.  Here we rely on Kafka's standard Consumer Group
             * behavior. That means the Kafka Broker itself keeps the "Current Offset". Therefore,
             * when useConsumerGroups=true and we "restart the service" (like after a reboot) the
             * data consumption process picks back up where at the offset the KafkaBroker gives us
             * (i.e., the KafkaBroker remembered how far each partition progressed)
             */
            List<String> topicList = newArrayList(options.pointTopic);
            kafkaConsumer.subscribe(topicList, new KafkaConsumerRebalanceListener());  //rely on std Kafka behavior
        } else {
            /*
             * This is the "non-standard" approach where we MANUALLY set the TopicPartitions we
             * will consume (e.g. "Partitions 1-10 of the Points Topic"). When useConsumerGroups=false
             * We force the KafkaConsumer to GET DATA FROM THE BEGINNING of the TopicPartitions.
             */
            List<TopicPartition> topicPartitions = options.topicPartitions();
            kafkaConsumer.assign(topicPartitions);  //enforce specific partitions.
            kafkaConsumer.seekToBeginning(topicPartitions);
        }
    }

    public static List<TopicPartition> makeTopicPartitions(int minPartition, int maxPartition, String topic) {

        checkState(minPartition >= 0, "minPartition must be at least 0");
        checkState(maxPartition >= 1, "maxPartition must be at least 1");
        checkState(minPartition < maxPartition, "minPartition must be < maxPartition");

        return IntStream.range(minPartition, maxPartition)
            .mapToObj(i -> new TopicPartition(topic, i))
            .collect(Collectors.toList());
    }

    /*
     * Put a task that pulls data from Kafka in the executor. This task reruns with a N-second gap
     * between each execution.
     */
    private void scheduleDataPullingTask() {

        /*
         * IMPORTANT: DO NOT use executor.scheduleAtFixedRate(task, initDelay, Period, TimeUnit)
         *
         * Fixed rate scheduling allows sequential executions of a long running task to begin
         * overlapping. Overlapping executions (A) waste resources and (B) introduces the very real
         * possibility of bugs caused by uninteded parallelism.
         */
        this.serviceAssets.mainExecutor().scheduleWithFixedDelay(
            new ErrorCatchingTask(new DataPullingTask(), serviceAssets.exceptionHandler()),
            5_000, //initial delay
            options.milliSecBtwPollAttempts, //delay between tasks
            TimeUnit.MILLISECONDS
        );
    }

    private class DataPullingTask implements Runnable {

        int numConsecutivePullsSkipped;

        DataPullingTask() {
            this.numConsecutivePullsSkipped = 0;
        }

        @Override
        public void run() {

            if (shouldPullMoreData()) {
                pullDataFromKafka();
                numConsecutivePullsSkipped = 0;
            } else {
                numConsecutivePullsSkipped++;
                warnIfTooManyCancels();
            }
        }

        /*
         * The method creates back-pressure that ensures DataPullingTasks WILL NOT retrieve so much
         * data that they crash the JVM with an OutOfMemoryException.
         */
        private boolean shouldPullMoreData() {
            return totalPointsInAllQueues() < options.pointPrefetchLimit;
        }

        private void pullDataFromKafka() {
            Duration timeout = Duration.ofSeconds(10);

            System.out.println("polling..");
            ConsumerRecords<String, KAFKA_VAL> records = kafkaConsumer.poll(timeout);
            System.out.println("done polling [" + records.count() + " records retrieved]");

            for (ConsumerRecord<String, KAFKA_VAL> record : records) {

                Optional<? extends Point> optional = converter.parse(record);

                if (optional.isPresent()) {
                    Point point = optional.get();

                    PK partitionKey = converter.partitionKeyFor(point);
                    swimLanes.get(partitionKey).offerToQueue(point);
                    latencyCollectors.get(partitionKey).incorporate(record, point);
                    ingestSummarizer.incorporate(record, point);
                }
            }
        }

        private void warnIfTooManyCancels() {
            final int LIMIT = 100;
            if (numConsecutivePullsSkipped >= LIMIT && numConsecutivePullsSkipped % LIMIT == 0) {
                throw new RuntimeException("Cannot pull more data from Kafka because too much data is already awaiting processing.  Current total queue size = " + totalPointsInAllQueues());
            }
        }
    }

    /* Use the total number of points currently held in memory to rate-limit data pulling. */
    private int totalPointsInAllQueues() {

        return swimLanes.values().stream()
            .mapToInt(lane -> lane.queueSize())
            .sum();
    }

    private void scheduleDataProcessingTasks() {

        /*
         * Schedule a "processing task" for EVERY partition even though it is unlikely that all
         * partition will be "pulling data". The extra tasks are "no-ops" that can be ignored. It is
         * much better to schedule extra no-op tasks than to run the risk that some data gets
         * pulled into memory and NEVER processed because there is no dedicated processing task.
         */
        for (PK partition : partitionMapping.partitionList()) {

            /*
             * IMPORTANT: DO NOT use executor.scheduleAtFixedRate(task, initialDelay, Period,
             * TimeUnit). Fixed rate scheduling allows sequential executions of a long running task
             * to begin overlapping. Overlapping executions (A) cause a cascade of wasted resources
             * and (B) introduces the very real possibility of bugs caused by uninteded parallelism.
             *
             *
             * ALSO IMPORTANT: The small (100ms) delay between subsequent executions of the same
             * task is intended to ensure worker threads will be FULLY saturated with data (assuming
             * data is available to be pulled and processed). If you want to prevent ARIA from fully
             * saturating every CPUs on the host machine set the number of worker threads to be less
             * than the number of host CPUs.
             */
            serviceAssets.mainExecutor().scheduleWithFixedDelay(
                createProcessingTaskFor(partition),
                5_000, //initial delay
                100, //Notice, this small 100ms delay between tasks.
                TimeUnit.MILLISECONDS
            );
        }
    }

    //create a Task that processes data for exactly one swimlane
    private Runnable createProcessingTaskFor(PK place) {

        SwimLane laneToProcess = swimLanes.get(place);
        DataProcessingTask coreTask = new DataProcessingTask(laneToProcess);
        /*
         * add error handling to ensure exceptions thrown inside coreTask do not kill a thread
         * inside the executor
         */
        return new ErrorCatchingTask(coreTask, serviceAssets.exceptionHandler());
    }

    private class DataProcessingTask implements Runnable {

        final SwimLane queueAndKpi;

        DataProcessingTask(SwimLane queueAndKpi) {
            this.queueAndKpi = queueAndKpi;
        }

        @Override
        public void run() {
            try {
                queueAndKpi.processQueuedData();
            } catch (Exception ex) {
                serviceAssets.exceptionHandler().handle("An Exception occurred when processing point data", ex);
            }
        }
    }

    private void scheduleIngestSummaryLogging() {

        ErrorCatchingTask task = new ErrorCatchingTask(
            () -> ingestSummarizer.writeLogs("logs"),
            serviceAssets.exceptionHandler()
        );

        serviceAssets.fastTaskExecutor().scheduleWithFixedDelay(
            task,
            1, //the initial delay before the first round of logging
            10, //the period between each subsequent round of logging
            TimeUnit.MINUTES
        );
    }

    //extract out the StreamingKpi to facilite aggregate logging
    public Map<PK, StreamingKpi> kpiMap() {
        Map<PK, StreamingKpi> kpis = newHashMap();
        for (Map.Entry<PK, SwimLane> entry : swimLanes.entrySet()) {
            kpis.put(entry.getKey(), entry.getValue().kpi());
        }
        return kpis;
    }

    public Map<PK, KafkaLatencyCollector> latencyCollectors() {
        return this.latencyCollectors;
    }


    /** Expose the SwimLanes so we can harvest metrics for logging. */
    public Map<PK, SwimLane> swimLanes() {
        return this.swimLanes;
    }

    /**
     * Schedule a fast-running Task to repeatedly execute at a fixed delay.
     *
     * @param runMe       A quick task (frequently log data collection or heart beat operations)
     * @param timeBtwRuns The "rest time" between subsequent executions of this task (also the delay
     *                    before the task is first executed)
     */
    public void scheduleQuickTask(Runnable runMe, Duration timeBtwRuns) {

        ErrorCatchingTask task = new ErrorCatchingTask(runMe, serviceAssets.exceptionHandler());

        serviceAssets.fastTaskExecutor().scheduleWithFixedDelay(
            task,
            timeBtwRuns.getSeconds(),
            timeBtwRuns.getSeconds(),
            TimeUnit.SECONDS
        );
    }

    /**
     * A RecordHelper converts the ConsumerRecords pulled from Kafka to Points that can be routed to
     * a SwimLane.
     *
     * @param <KFK_VAL> The KafkaConsumer Value parameter
     * @param <PK>      The Partition Key (Facility or AsdexAirport)
     */
    public interface RecordHelper<KFK_VAL, PK> {

        Optional<? extends Point> parse(ConsumerRecord<String, KFK_VAL> consumerRecord);

        PK partitionKeyFor(Point point);
    }

    public static class NopPlugin implements RecordHelper<String, Facility> {

        @Override
        public Optional<NopPoint> parse(ConsumerRecord<String, String> consumerRecord) {

            String lineOfInput = consumerRecord.value();

            if (isNopRadarHit(lineOfInput)) {
                return parseSafely(lineOfInput);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public Facility partitionKeyFor(Point point) {
            return toFacility(point.facility());
        }
    }

    public static RecordHelper<String, Facility> nopPlugin() {
        return new NopPlugin();
    }

    /**
     * This listener is invoked every time the associated Kafka consumer is affected by a re-balance
     * operation. When invoked, the listener flushes every StreamingKpi and clears every
     * BlockingQueue belonging to facility partition that has been reassigned to another consumer.
     */
    class KafkaConsumerRebalanceListener implements ConsumerRebalanceListener {

        private final Collection<TopicPartition> recentlyRevoked;

        public KafkaConsumerRebalanceListener() {
            this.recentlyRevoked = newArrayList();
        }

        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
            /*
             * This method is called as part of a somewhat confusing two-step process.
             *
             * Step 1 = onPartitionsRevoked(Collection<TopicPartition>) - Tells the Consumer it is no
             * longer responsible for processing these TopicPartitions
             *
             * Step 2 = onPartitionsAssigned(Collection<TopicPartition>) - Tells the Consumer it is now
             * responsible for processing these TopicPartitions
             *
             * The confusing part is that many TopicPartition are frequently "revoked" and then
             * immediately (re)"assigned". Consequently, you should not flush any SwimLanes UNTIL we
             * verify the "reassignment" does not occur. This is feasible because BOTH steps are always
             * called and the STEP 1 always occurs first.
             */
            this.recentlyRevoked.addAll(revokedPartitions);
        }

        @Override
        public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
            //SEE COMMENT IN "onPartitionsRevoked" to see how these two methods inter-relate

            //identify the TopicPartition that were revoked BUT NOT immediately reassigned
            this.recentlyRevoked.removeAll(assignedPartitions);

            for (TopicPartition trulyRevokedTp : recentlyRevoked) {

                //Optional because you may see extra "empty partitions" your partitionMapping has no data for...
                Optional<PK> reassignedFacility = partitionMapping.itemForPartition(
                    trulyRevokedTp.partition()
                );

                //Ignore any "un mapped partitions"
                if (!reassignedFacility.isPresent()) {
                    continue;
                }

                /*
                 * Do not IMMEDIATELY flush the KPI because this leads to unwanted parallelism.
                 * Instead, we tell the SwimLane that the next time it is "processed" to flush the
                 * data afterward
                 */
                SwimLane lane = swimLanes.get(reassignedFacility.get());
                lane.scheduleFlushOnNextExecution();
            }
            recentlyRevoked.clear();
        }
    }


    public static Options optionsFromYaml(File yamlFile) throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        //Parse out a Builder...then build it.
        return mapper.readValue(yamlFile, KafkaIngestor.Options.class);
    }


    /**
     * This Options class is part of a multi-step refactor to transition from Properties to YAML.
     * This class can be built by extracting values from a Properties object OR by parsing a YAML
     * file.
     *
     * <p>Eventually, the "Properties" code path will be removed completely.
     */
    public static class Options {

        /**
         * Pulling more data is OK if the total amount of currently enqueued data is less than
         * this.
         */
        public final Integer pointPrefetchLimit;

        /** The number of threads in the executor that does all the work. */
        public final Integer numWorkerThreads;

        /**
         * Delay btw two consecutive DataPullingTask executions (time starts counting AFTER a task
         * finishes)
         */
        public final Integer milliSecBtwPollAttempts;

        public final Boolean useConsumerGroups;

        /**
         * Only needed when useConsumerGroups is false (and partitions are assigned instead of
         * subscribed to).
         */
        public final Integer minPartition;

        /**
         * Only needed when useConsumerGroups is false (and partitions are assigned instead of
         * subscribed to).
         */
        public final Integer maxPartition;

        /** The name of kafka topic where raw point data is found. */
        public final String pointTopic;

        public Options(Properties properties) {
            this.pointPrefetchLimit = getInt("point.prefetch.limit", properties);
            this.numWorkerThreads = getInt("numWorkerThreads", properties);
            this.milliSecBtwPollAttempts = getOptionalInt("data.pull.freq.ms", properties, 2_000);
            this.useConsumerGroups = getOptionalBoolean("useConsumerGroups", properties, false);
            this.minPartition = getInt("min.consumer.partition", properties);
            this.maxPartition = getInt("max.consumer.partition", properties);
            this.pointTopic = getString("kafkaTopicName", properties);
        }

        public Options() {
            this.pointPrefetchLimit = 500_000;
            this.numWorkerThreads = 4;
            this.milliSecBtwPollAttempts = 2_000;
            this.useConsumerGroups = true; //when false, must supply min & max partition
            this.minPartition = null;
            this.maxPartition = null;
            this.pointTopic = null; //this should be set via YAML
        }

        List<TopicPartition> topicPartitions() {
            checkState(!useConsumerGroups, "TopicPartitions are only required when manually assigning partitions");
            return makeTopicPartitions(minPartition, maxPartition, pointTopic);
        }
    }
}
