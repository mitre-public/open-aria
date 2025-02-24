/*
 * NOTICE:
 * This is the copyright work of The MITRE Corporation, and was produced for the
 * U. S. Government under Contract Number DTFAWA-10-C-00080, and is subject to
 * Federal Aviation Administration Acquisition Management System Clause 3.5-13,
 * Rights In Data-General, Alt. III and Alt. IV (Oct. 1996).
 *
 * No other use other than that granted to the U. S. Government, or to those
 * acting on behalf of the U. S. Government, under that Clause is authorized
 * without the express written permission of The MITRE Corporation. For further
 * information, please contact The MITRE Corporation, Contracts Management
 * Office, 7515 Colshire Drive, McLean, VA  22102-7539, (703) 983-6000.
 *
 * Copyright 2020 The MITRE Corporation. All Rights Reserved.
 */
package org.mitre.openaria.kafka;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Supplier;

import org.mitre.caasd.commons.out.JsonWritable;
import org.mitre.caasd.commons.out.OutputSink;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;

/**
 * A KafkaOutputSink converts incoming items to String (typically JSON) and sends them to a Kafka
 * topic.
 *
 * <p>Note, If you also want to write these items to File by composing this OutputSink with a
 * JsonFileSink or a PrintStreamSink using the "andThen" method.
 */
public class KafkaOutputSink<T extends JsonWritable> implements OutputSink<T> {

    /**
     * Communicates with the Kafka Cluster, the keys (Strings) the values (String) are
     * CompleteEvents stored as JSON.
     */
    private final KafkaProducer<String, String> kafkaProducer;

    /**
     * This factory converts an instance of T into a ProducerRecord that can be sent to Kafka. The
     * resulting ProducerRecords "package up" a Key, Value, topic, partition number, and
     * KafkaHeaders into one item that is suitable to send over the wire.
     */
    private final ProducerRecordFactory<String, String, T> recordMaker;

    /** These callbacks are called when an Event is successfully (or not) published to Kafka. */
    private final Supplier<Callback> callbackSupplier;

    /**
     * Create a KafkaOutputDestination that automatically pipes Events to Kafka and emits warnings
     * when a message fails to be published properly
     *
     * @param recordMaker   Controls which {topic, key, partition} each item is sent to Kafka
     * @param kafkaProducer A Kafka client that publishes records to the Kafka cluster
     */
    public KafkaOutputSink(
        ProducerRecordFactory<String, String, T> recordMaker,
        KafkaProducer<String, String> kafkaProducer
    ) {
        this(recordMaker, kafkaProducer, warnIfSendFails());
    }

    /**
     * Create a KafkaOutputDestination that automatically pipes Events to Kafka.
     *
     * @param recordMaker      Controls which {topic, key, partition} each item is sent to Kafka
     * @param kafkaProducer    A Kafka client that publishes records to the Kafka cluster
     * @param callbackSupplier A custom "upon publication" Callback supplier
     */
    public KafkaOutputSink(
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
    }

    /**
     * A KafkaProducer can use this Callback function via the "kafkaProducer.send(ProducerRecord,
     * Callback)" method. This method ensures failures to write to a Kafka log are noticed. Failing
     * writes to Kafka WILL go unnoticed unless either (A) a callback is used or (B) the Future
     * returned by send(ProducerRecord) is queried.
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
}
