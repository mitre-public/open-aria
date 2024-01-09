

package org.mitre.openaria.airborne;

import static java.lang.Double.compare;
import static org.mitre.openaria.system.FacilitySet.allFacilites;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AirborneEvents {

    /*
     * Multiple attempts are REQUIRED because fetching data from Kafka can sometime take a moment to
     * begin. This occurs when the Kafka-broker needs to confirm it has up-to-date information
     * BEFORE returning results. This confirmation process can take long enough that the first 1 or
     * 2 attempt to retrieve data will time-out and return no data even though there is plenty of
     * data to retrieve. So, don't give-up too quickly
     */
    private static final int REQUIRED_ATTEMPTS_WITH_NO_RESULTS = 5;

    /**
     * @param kafkaProperties
     * @param includeThese
     * @param maxEventCount
     *
     * @return
     */
    public static Collection<AirborneEvent> getAirborneEventsFromKafka(
        Properties kafkaProperties,
        Predicate<AirborneEvent> includeThese,
        int maxEventCount
    ) {

        AirborneEventGrabber kafkaDataGrabber = new AirborneEventGrabber(kafkaProperties, allFacilites());

        TreeSet<AirborneEvent> allEvents = new TreeSet<>(
            (AirborneEvent e1, AirborneEvent e2) -> compare(e1.score(), e2.score())
        );

        ArrayList<AirborneEvent> newEvents = null;

        int totalEventCount = 0;
        int numConsecutiveEmptyPulls = 0;

        do {
            newEvents = kafkaDataGrabber.getEvents();

            if (newEvents.isEmpty()) {
                numConsecutiveEmptyPulls++;
            } else {
                numConsecutiveEmptyPulls = 0;
            }

            totalEventCount += newEvents.size();

            System.out.println("Found " + newEvents.size() + " more events -- totaling = " + totalEventCount);

            List<AirborneEvent> goodEvents = newEvents.stream()
                .filter(includeThese)
                .collect(Collectors.toList());

            System.out.println("Of these: " + goodEvents.size() + " are good ");

            allEvents.addAll(goodEvents);

            if (maxEventCount > 0) {
                while (allEvents.size() > maxEventCount) {
                    allEvents.pollLast();
                }
            }

            if (allEvents.size() > 0) {
                System.out.println("Top Event Score: " + allEvents.first().score());
                System.out.println("Last Event Score: " + allEvents.last().score());
            }

        } while (numConsecutiveEmptyPulls < REQUIRED_ATTEMPTS_WITH_NO_RESULTS);

        System.out.println("Done pulling events from Kafka");

        return allEvents;
    }

    /**
     * Fetch ALL the AirborneEvents from kafka that pass the provided predicate. Be careful, this
     * call can easily blow-out memory if you request too many events.
     *
     * @param kafkaProperties The properties file is used to set of a Kafka Consumer
     * @param includeThese    This predicate is filters which events are returned.
     *
     * @return
     */
    public static Collection<AirborneEvent> getAirborneEventsFromKafka(
        Properties kafkaProperties,
        Predicate<AirborneEvent> includeThese
    ) {
        return AirborneEvents.getAirborneEventsFromKafka(kafkaProperties, includeThese, -1);
    }

    /**
     * Apply a Consumer to ALL qualifying Airborne events that are found in a Kafka cluster.
     *
     * <p>This method applies the Consumer as the data is retrieved from Kafka. This "retrieve a
     * few,
     * process a few" paradigm is preferable to a "retrieve ALL, process ALL" paradigm when the
     * Kafka cluster contains a very large number of events. The "retrieve ALL" paradigm can fail
     * due to OutOfMemoryException when there are too many qualifying events.
     *
     * <p>This method can be used to do things like: make a week's worth of Falcon Bookmarks, make
     * a
     * day's worth of Cedar EORs, or collect aggregate statistics on all airborne events that match
     * a specific predicate.
     *
     * @param task            A consumer that will receive all qualifying Airborne events
     * @param includeThese    This predicate is filters which events are returned.
     * @param kafkaProperties This properties file sets up a KafkaConsumer
     */
    public static void processAirborneEventsInStream(
        Consumer<AirborneEvent> task,
        Predicate<AirborneEvent> includeThese,
        Properties kafkaProperties
    ) {

        AirborneEventGrabber kafkaDataGrabber = new AirborneEventGrabber(
            kafkaProperties,
            allFacilites()
        );

        int eventCount = 0;
        int qualifyingEventCount = 0;
        ArrayList<AirborneEvent> newEvents;

        int numConsecutiveEmptyPulls = 0;

        do {
            newEvents = kafkaDataGrabber.getEvents();

            if (newEvents.isEmpty()) {
                numConsecutiveEmptyPulls++;
            } else {
                numConsecutiveEmptyPulls = 0;
            }

            eventCount += newEvents.size();

            System.out.println("Found " + newEvents.size() + " more events.  Total events found: " + eventCount);

            List<AirborneEvent> goodEvents = newEvents.stream()
                .filter(includeThese)
                .collect(Collectors.toList());

            qualifyingEventCount += goodEvents.size();

            System.out.println("Of these: " + goodEvents.size() + " qualify.  Total qualifying events: " + qualifyingEventCount);

            //apply the Consumer to each qualifying event
            goodEvents.forEach(task);

        } while (numConsecutiveEmptyPulls < REQUIRED_ATTEMPTS_WITH_NO_RESULTS);

        System.out.println("Done pulling events from Kafka");
    }
}
