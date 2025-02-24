package org.mitre.openaria.kafka;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;

/**
 * A KafkaEmitter is a wrapper for a KafkaProducer that makes two common "Kafka-ish" tasks
 * injectable Strategy Objects.
 * <p>
 * Task 1 = "Respond appropriately when sending a message to Kafka fails or succeeds" Task 2 =
 * "Convert an outgoing object T into a fully-fledged ProducerRecord with the correct {topic, key,
 * value, partition, and KafkaHeader}"
 * <p>
 * A KafkaEmitter allows client code to easily inject dependencies that perform these two tasks.
 * <p>
 * DEVELOPER NOTE: This class assumes we want to write to Kafka topics that use Strings for both the
 * Keys and Values. This assumption can be retracted by making the Key and Value types generic.
 * However, we are choosing not to support this level of flexibility because (1) multiple generic
 * types make code confusing to read AND (2)we have no plans to write non-String Keys or Values
 *
 * @param <T> The type of object that will be sent to Kafka (after converting to a String message)
 */
public class KafkaEmitter<T> implements Consumer<T> {

    /** Communicates with the Kafka Cluster, the keys (Strings) the values (String). */
    private final KafkaProducer<String, String> kafkaProducer;

    /**
     * This factory creates a ProducerRecord for each String it wants to emit. The resulting
     * ProducerRecords "package up" a Key, Value, topic, partition number, and KafkaHeaders into one
     * item that is suitable to send over the wire.
     */
    private final ProducerRecordFactory<String, String, T> recordMaker;

    /**
     * These callbacks are called when outgoing messages are successfully (or not) published to
     * Kafka.
     */
    private final Supplier<Callback> callbackSupplier;

    /**
     * Create a KafkaStringEmitter that relies on a ProducerRecordFactory to generate the correct:
     * topic, key, partition, and KafkaHeader to use when sending data to Kafka.
     * <p>
     * This KafkaStringEmitter will emit warnings when a message fails to be published properly.
     *
     * @param recordMaker   Controls what {topic, key, partition, and header} is used for each
     *                      message sent to Kafka.
     * @param kafkaProducer A Kafka client that publishes records to the Kafka cluster
     */
    public KafkaEmitter(
        ProducerRecordFactory<String, String, T> recordMaker,
        KafkaProducer<String, String> kafkaProducer
    ) {
        this(recordMaker, kafkaProducer, warnIfSendFails());
    }

    /**
     * Create a KafkaStringEmitter that relies on the ProducerRecordFactory and Callback Supplier
     * provided here.
     * <p>
     * The ProducerRecordFactory generates the correct: topic, key, partition, and KafkaHeader to
     * use when sending data to Kafka.
     * <p>
     * Callbacks gotten from the callbackSupplier are used when a "send to Kafka call" resolves.
     * Both successful sends and failed send attempts use the same Callback.
     *
     * @param recordMaker      Controls what {topic, key, partition, and header} is used for each
     *                         message sent to Kafka.
     * @param kafkaProducer    A Kafka client that publishes records to the Kafka cluster
     * @param callbackSupplier A custom "upon publication" Callback supplier that is triggered when
     *                         records are successfully sent or fail to be sent.
     */
    public KafkaEmitter(
        ProducerRecordFactory<String, String, T> recordMaker,
        KafkaProducer<String, String> kafkaProducer,
        Supplier<Callback> callbackSupplier
    ) {
        this.recordMaker = checkNotNull(recordMaker);
        this.callbackSupplier = checkNotNull(callbackSupplier);
        this.kafkaProducer = checkNotNull(kafkaProducer);
    }

    @Override
    public void accept(T item) {

        ProducerRecord<String, String> record = recordMaker.producerRecordFor(item);

        //Have the callback supplier generate a new callback for this single record
        kafkaProducer.send(record, callbackSupplier.get());

        /*
         * IMPORTANT NOTE: The Future<RecordMetadata> returned by kafkaProducer.send(...) is NOT
         * being used here. This means we need to rely on the call back function to "notice/handle"
         * successful sends and failed send attempts. The default behavior is to throw exceptions
         * when sends fail so at least the absence of exceptions will inform us that the send was
         * successful.
         */
    }

    /**
     * This Callback function is called when the "kafkaProducer.send(ProducerRecord, Callback)"
     * method resolves in a failure or a success. This method ensures failures to write to a Kafka
     * can be acknowledged and handled. Failing writes to Kafka WILL go unnoticed unless a callback
     * of some kind is used.
     */
    private static class WarnIfSendFails implements Callback {

        @Override
        public void onCompletion(RecordMetadata rm, Exception ex) {
            if (ex != null) {
                throw new KafkaException("Failure when sending data to Kafka", ex);
            }
        }
    }

    /** This Supplier provides Callbacks that emit warnings if a "Kafka send" fails; */
    public static Supplier<Callback> warnIfSendFails() {
        return () -> new WarnIfSendFails();
    }

    /**
     * This Factory makes ProducerRecords that will route data to the provided topic. However, no
     * Partition information, Header information, or Keys are provided.
     */
    public static <T> ProducerRecordFactory<String, String, T> emitToTopic(final String topic) {
        return (T messageForKafka) -> new ProducerRecord(topic, messageForKafka);
    }

}
