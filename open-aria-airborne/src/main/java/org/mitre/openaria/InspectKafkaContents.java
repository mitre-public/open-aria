package org.mitre.openaria;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.airborne.AirborneEvents.processAirborneEventsInStream;
import static org.mitre.caasd.commons.Functions.ALWAYS_TRUE;
import static org.mitre.caasd.commons.util.PropertyUtils.getString;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;

import java.util.Properties;

import org.mitre.openaria.airborne.EventStatisticsCollector;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.parsing.nop.Facility;

/**
 * The purpose of this program is to pull ALL the Airborne event data in Kafka and provide a summary
 * of the data pulled.
 */
public class InspectKafkaContents {

    //This property must exist in the property file
    private static final String ACTIVITY_LOG_FILE = "logFile";

    public static void main(String[] args) throws Exception {
        checkArgument(args.length == 1, "Usage: java -cp ARIA.jar org.mitre.openaria.InspectKafkaContents PROPERTY_FILE");

        final Properties props = loadProperties(args[0]);
        final String logFile = getString(ACTIVITY_LOG_FILE, props);

        EventStatisticsCollector dataCollector = new EventStatisticsCollector();

        /*
         * Publish the events as you pull them from Kafka. DO NOT pull all the events AND THEN
         * publish them to CEDAR, this can cause an OutOfMemoryException
         */
        processAirborneEventsInStream(
            dataCollector,
            ALWAYS_TRUE,
            props
        );

        writeLog(dataCollector, logFile);
    }

    private static void writeLog(EventStatisticsCollector dataCollector, String logFile) throws Exception {

        StringBuilder sb = new StringBuilder();

        for (Facility facility : Facility.values()) {
            sb.append(dataCollector.makeFacilityReport(facility));
        }

        sb.append(dataCollector.makeSummaryReport());

        FileUtils.appendToFile(logFile, sb.toString());
    }
}
