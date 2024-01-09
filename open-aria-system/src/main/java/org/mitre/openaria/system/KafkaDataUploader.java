
package org.mitre.openaria.system;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Long.parseLong;
import static org.mitre.openaria.system.ExceptionHandlers.sequentialFileWarner;
import static org.mitre.caasd.commons.parsing.nop.Facility.toFacility;
import static org.mitre.caasd.commons.util.FilteredIterator.filter;
import static org.mitre.caasd.commons.util.PropertyUtils.getString;

import java.io.File;
import java.time.Duration;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.mitre.openaria.core.ApproximateTimeSorter;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.kafka.FacilityPartitionMapping;
import org.mitre.caasd.commons.parsing.nop.NopParser;
import org.mitre.caasd.commons.util.ExceptionHandler;

import kafka.common.KafkaException;

/**
 * A KafkaDataUploader ensures raw NOP data files are uploaded to Kafka. Once the data is uploaded
 * the file is deleted.
 */
public class KafkaDataUploader implements Consumer<File> {

    /**
     * A KafkaProducer that uses Strings for both the Key and Value. Note, the Key is not used
     * because the "points" topic created here does not assign a unique key to each point.
     */
    private final KafkaProducer<String, String> kafkaProducer;

    private final FacilityPartitionMapping partitionMapping;

    private final String topicName;

    private final ExceptionHandler exceptionHandler;

    /** How far is the "look ahead" when incoming files a pre-sorted before being uploaded to Kafka. */
    private final Duration sortDuration;

    /** Callbacks are called after a message is published to Kafka (called on success and failure). */
    private final Supplier<Callback> callbackSupplier;

    public KafkaDataUploader(Properties combinedProps, String topicName, FacilityPartitionMapping partitionMapping) {
        checkNotNull(combinedProps);
        this.topicName = checkNotNull(topicName);
        this.partitionMapping = checkNotNull(partitionMapping);
        this.kafkaProducer = new KafkaProducer<>(combinedProps);
        this.exceptionHandler = sequentialFileWarner("warnings");

        this.sortDuration = Duration.ofSeconds(
            parseLong(getString("ingestSortDurationInSec", combinedProps))
        );

        this.callbackSupplier = () -> new WarnIfSendFails();
    }

    @Override
    public void accept(File file) {

        System.out.println("Uploading: " + file.getAbsolutePath() + " to Kafka");

        uploadFileToKafka(file);
        file.delete();
    }

    /*
     * Note: This would be the place to parallelize the data upload process. The only thing we would
     * want to confirm is that exactly one file is being uploaded to a facility at any given time.
     */
    private synchronized void uploadFileToKafka(File inputFile) {

        //use a try-with-resources block to ensure this NopParser is closed properly
        try (NopParser parser = new NopParser(inputFile)) {
            ingestFile(parser);
        } catch (Exception ex) {
            exceptionHandler.handle(
                "An Exception occured when processing the file: " + inputFile.getAbsolutePath(),
                ex
            );
        }
    }

    private void ingestFile(final NopParser parser) {
        /*
         * Help prevent out of order data by caching 2.5 minutes of Point data before uploading the
         * oldest known Point to Kafka. This step cannot guarantee that data upload to Kafka will be
         * in perfect time order, but it will correct small out-of-time-sequence "errors"
         */
        ApproximateTimeSorter<Point> pointSorter = new ApproximateTimeSorter<>(
            sortDuration,
            (Point p) -> publishPointToKafka(p)
        );

        PointIterator completeIterator = new PointIterator(parser);

        Iterator<Point> filterIter = filter(
            completeIterator,
            (Point p) -> p.hasTrackId()
        );

        while (filterIter.hasNext()) {
            Point next = filterIter.next();
            pointSorter.accept(next);
        }
        pointSorter.flush();
        kafkaProducer.flush();
    }

    private void publishPointToKafka(Point p) {

        ProducerRecord<String, String> record = new ProducerRecord<>(
            topicName,
            partitionMapping.partitionFor(toFacility(p.facility())).get(),
            null, //a Key is not used
            p.asNop()
        );

        //Have the callback supplier generate a new callback for this single record
        Future<RecordMetadata> future = kafkaProducer.send(record, callbackSupplier.get());
    }


    /**
     * A KafkaProducer can use this Callback function via the "kafkaProducer.send(ProducerRecord,
     * Callback)" method. This method ensures failures to write to a Kafka log are noticed. Failing
     * writes to Kafka WILL go unnoticed unless either (A) a callback is used or (B) the Future
     * returned by the send(ProducerRecord) method is queried.
     */
    private static class WarnIfSendFails implements Callback {

        @Override
        public void onCompletion(RecordMetadata rm, Exception ex) {
            if (ex != null) {
                throw new KafkaException("Failure when sending data to Kafka", ex);
            }
        }
    }
}