package org.mitre.openaria.system;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;

public class KafkaConsumerUtils {





//    /* Build a KafkaConsumer that relies on Kafka's ConsumerGroup feature to distribute work. */
//    public static <KAFKA_VAL> KafkaConsumer<String, KAFKA_VAL> makeKafkaConsumerWithAutoPartitionBalancing(
//        Properties kafkaProps,
//        String topic,
//        ConsumerRebalanceListener rebalanceListener
//    ) {
//        verifyKafkaBrokers(kafkaProps);
//
//        checkState(
//            kafkaProps.containsKey(PARTITION_ASSIGNMENT_STRATEGY_CONFIG),
//            "The Kafka Properties config must contain a partition.assignment.strategy"
//        );
//
//        KafkaConsumer<String, KAFKA_VAL> kc = new KafkaConsumer(kafkaProps);
//
//        kc.subscribe(newArrayList(topic), rebalanceListener);
//
//        //DO NOT DO THIS...
//        //We NEVER call "seekToBeginning" because all rebalance calls will "snap" to live data.
//        //kc.seekToBeginning(kc.assignment());
//        return kc;
//    }
//
//    /* Build a KafkaConsumer that relies on manually specified Partition assignment. */
//    public static <KAFKA_VAL> KafkaConsumer<String, KAFKA_VAL> makeKafkaConsumerWithFixedPartitions(
//        Properties kafkaProps,
//        List<TopicPartition> topicPartitions
//    ) {
//        KafkaConsumer<String, KAFKA_VAL> kc = new KafkaConsumer(kafkaProps);
//
//        kc.assign(topicPartitions);
//        kc.seekToBeginning(topicPartitions);
//        return kc;
//    }
//
//
//    public static List<TopicPartition> makeTopicPartitions(int minPartition, int maxPartition, String topic) {
//
//        checkState(minPartition >= 0, "minPartition must be at least 0");
//        checkState(maxPartition >= 1, "maxPartition must be at least 1");
//        checkState(minPartition < maxPartition, "minPartition must be < maxPartition");
//
//        return IntStream.range(minPartition, maxPartition)
//            .mapToObj(i -> new TopicPartition(topic, i))
//            .collect(Collectors.toList());
//    }
//
//
//
//    public static class AutoBalancingKafka<KAFKA_VAL>  implements Supplier<KafkaConsumer<String, KAFKA_VAL>> {
//
//        Properties kafkaProps;
//        String topic;
//        ConsumerRebalanceListener rebalanceListener;
//
//        public AutoBalancingKafka(
//            Properties kafkaProps,
//            String topic,
//            ConsumerRebalanceListener rebalanceListener
//        ) {
//            this.kafkaProps = requireNonNull(kafkaProps);
//            this.topic = requireNonNull(topic);
//            this.rebalanceListener = requireNonNull(rebalanceListener);
//        }
//
//        @Override
//        public KafkaConsumer<String, KAFKA_VAL> get() {
//
//            verifyKafkaBrokers(kafkaProps);
//
//            checkState(
//                kafkaProps.containsKey(PARTITION_ASSIGNMENT_STRATEGY_CONFIG),
//                "The Kafka Properties config must contain a partition.assignment.strategy"
//            );
//
//            KafkaConsumer<String, KAFKA_VAL> kc = new KafkaConsumer(kafkaProps);
//
//            kc.subscribe(newArrayList(topic), rebalanceListener);
//
//            //DO NOT DO THIS...
//            //We NEVER call "seekToBeginning" because all rebalance calls will "snap" to live data.
//            //kc.seekToBeginning(kc.assignment());
//            return kc;
//        }
//    }
}
