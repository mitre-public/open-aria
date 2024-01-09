package org.mitre.openaria.kafka;

import java.util.List;
import java.util.Optional;

/**
 * A PartitionMapping provides a Kafka Partition number for an arbitrary item of type {@code T}, as
 * well as an item (of type {@code T}) for an arbitrary partition (i.e., a bidirectional map).
 */
public interface PartitionMapping<T> {

    Optional<Integer> partitionFor(T item);

    Optional<T> itemForPartition(int partition);

    List<T> partitionList();
}
