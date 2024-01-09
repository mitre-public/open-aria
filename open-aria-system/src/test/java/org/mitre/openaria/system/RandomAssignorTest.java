package org.mitre.openaria.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.internals.PartitionAssignor.Subscription;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;

public class RandomAssignorTest {

    RandomAssignor testAssignor = new RandomAssignor();

    @Test
    public void ConsumerEmptyWithoutTopic() {
        String consumerId = "testConsumer";
        Map<String, Integer> partitionsPerTopic = new HashMap<>();
        Map<String, List<TopicPartition>> assignment = testAssignor.assign(
            partitionsPerTopic,
            Collections.singletonMap(consumerId, new Subscription(Collections.<String>emptyList()))
        );

        assertEquals(Collections.singleton(consumerId), assignment.keySet());
        assertTrue(assignment.get(consumerId).isEmpty());
    }

    @Test
    public void assignmentWorksWithMultipleConsumers() {
        String topic = "testTopic";
        List<String> topicList = new ArrayList<String>();
        topicList.add(topic);
        String consumerId1 = "testConsumer1";
        String consumerId2 = "testConsumer2";
        Map<String, Integer> partitionsPerTopic = new HashMap<>();
        partitionsPerTopic.put(topic, 1);
        Map<String, Subscription> consumers = new HashMap<>();
        consumers.put(consumerId1, new Subscription(topicList));
        consumers.put(consumerId2, new Subscription(topicList));

        Map<String, List<TopicPartition>> assignment = testAssignor.assign(partitionsPerTopic, consumers);
        List<TopicPartition> testAssignment = new ArrayList<>();
        testAssignment.add(new TopicPartition(topic, 0));

        if (assignment.get(consumerId1).equals(Collections.<String>emptyList()) && assignment.get(consumerId2).equals(testAssignment)) {
            return;
        } else if (assignment.get(consumerId1).equals(testAssignment) && assignment.get(consumerId2).equals(Collections.<String>emptyList())) {
            return;
        } else {
            fail("Partition should be assigned to a single consumer");
        }
    }

    @Test
    public void onlyPartitionsFromSubscribedTopicsAreAssigned() {
        String topic1 = "testTopic1";
        String topic2 = "testTopic2";
        List<String> topicList = new ArrayList<String>();
        topicList.add(topic1);
        String consumerId = "testConsumer";
        Map<String, Integer> partitionsPerTopic = new HashMap<>();
        partitionsPerTopic.put(topic1, 3);
        partitionsPerTopic.put(topic2, 3);

        Map<String, List<TopicPartition>> assignment = testAssignor.assign(partitionsPerTopic,
            Collections.singletonMap(consumerId, new Subscription(topicList)));

        List<TopicPartition> testAssignment = new ArrayList<>();
        testAssignment.add(new TopicPartition(topic1, 0));
        testAssignment.add(new TopicPartition(topic1, 1));
        testAssignment.add(new TopicPartition(topic1, 2));

        assertEquals(testAssignment, assignment.get(consumerId));
    }

    @Test
    public void assignmentWorksWithMultipleTopics() {
        String topic1 = "testTopic1";
        String topic2 = "testTopic2";
        String topic3 = "testTopic3";
        List<String> topicList = new ArrayList<String>();
        topicList.add(topic1);
        topicList.add(topic2);
        topicList.add(topic3);
        String consumerId = "testConsumer";
        Map<String, Integer> partitionsPerTopic = new HashMap<>();
        partitionsPerTopic.put(topic1, 1);
        partitionsPerTopic.put(topic2, 1);
        partitionsPerTopic.put(topic3, 1);

        Map<String, List<TopicPartition>> assignment = testAssignor.assign(partitionsPerTopic,
            Collections.singletonMap(consumerId, new Subscription(topicList)));

        List<TopicPartition> testAssignment = new ArrayList<>();
        testAssignment.add(new TopicPartition(topic1, 0));
        testAssignment.add(new TopicPartition(topic2, 0));
        testAssignment.add(new TopicPartition(topic3, 0));

        assertEquals(testAssignment, assignment.get(consumerId));
    }
}
