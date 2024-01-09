package org.mitre.openaria.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.kafka.clients.consumer.internals.AbstractPartitionAssignor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.utils.Utils;

/**
 * This partition assignment strategy randomly assigns partitions to Kafka consumers w/in a consumer
 * group (whenever a consumer is added or removed from the group, ie re-balance)
 */
public class RandomAssignor extends AbstractPartitionAssignor {

    public RandomAssignor() {
    }

    @Override
    public Map<String, List<TopicPartition>> assign(Map<String, Integer> partitionsPerTopic, Map<String, Subscription> subscriptions) {
        Map<String, List<TopicPartition>> assignment = new HashMap();
        Iterator subIter = subscriptions.keySet().iterator();

        //initialize subscription mappings for assignment
        while (subIter.hasNext()) {
            String memberId = (String) subIter.next();
            assignment.put(memberId, new ArrayList());
        }

        ArrayList<String> consumerList = new ArrayList(Utils.sorted(subscriptions.keySet()));
        Iterator partIter = this.allPartitionsSorted(partitionsPerTopic, subscriptions).iterator();

        //assign partitions at random
        while (partIter.hasNext()) {
            TopicPartition partition = (TopicPartition) partIter.next();
            String topic = partition.topic();

            int rand = ThreadLocalRandom.current().nextInt(0, consumerList.size());
            while (!((Subscription) subscriptions.get(consumerList.get(rand))).topics().contains(topic)) {
                rand = ThreadLocalRandom.current().nextInt(0, consumerList.size());
            }

            (assignment.get(consumerList.get(rand))).add(partition);
        }

        return assignment;
    }

    /** Same as method from RoundRobinAssignor.class */
    public List<TopicPartition> allPartitionsSorted(Map<String, Integer> partitionsPerTopic, Map<String, Subscription> subscriptions) {
        SortedSet<String> topics = new TreeSet();
        Iterator subIter = subscriptions.values().iterator();

        while (subIter.hasNext()) {
            Subscription subscription = (Subscription) subIter.next();
            topics.addAll(subscription.topics());
        }

        List<TopicPartition> allPartitions = new ArrayList();
        Iterator topIter = topics.iterator();

        while (topIter.hasNext()) {
            String topic = (String) topIter.next();
            Integer numPartitionsForTopic = (Integer) partitionsPerTopic.get(topic);
            if (numPartitionsForTopic != null) {
                allPartitions.addAll(AbstractPartitionAssignor.partitions(topic, numPartitionsForTopic));
            }
        }

        return allPartitions;
    }

    @Override
    public String name() {
        return "random";
    }
}
