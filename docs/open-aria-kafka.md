## The `open-aria-kafka` module

### Purpose of Module

This module:

1. Interacts with Kafka, both as a Producer and Consumer
2. Hide details of Kafka all other code

### Important Classes within this Module

- `KafkaEmitter` and  `KafkaStringEmitter` these two classes pre-configure a `KafkaProducer` and adapt it into a `Consumer<T>`. This allows code to more seamlessly interact with Kafka.
- `KafkaOutputSink`, similar to `KafkaStringEmitter` -- but presumes input data can be converted to JSON
- `FacilityPartitionMapping` helps keep track of Kafka Partition numbers (for NOP facilities)

### Notes on Module

- As ARIA code has evolved we want fewer and fewer pieces of code interacting directly with the Kafka API. The Kafka API is easy to use but making the Facade classes (`KafkaEmitter`, `KafkaStringEmitter`, `KafkaOutputSink`) definitely simplify downstream code.


- Potential issue / Interesting Fact. No KafkaConsumer behavior has been migrated to this module. Is consuming data from Kafka easy enough that no Facade is necessary?

### Important Main methods & Launch Points

- none

