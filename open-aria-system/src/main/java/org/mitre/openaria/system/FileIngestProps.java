
package org.mitre.openaria.system;

import static com.google.common.collect.Lists.newArrayList;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.mitre.caasd.commons.util.ImmutableConfig;

/**
 * A FileIngestProps is a Properties file that contains all the asProperties required to configure
 * how a near real-time stream of incoming raw data (usually NOP and ASDE) is handled.
 */
public class FileIngestProps {

    //REQUIRED PROPERTIES
    public static final String LOCATION_OF_RAW_DATA = "locationOfRawData";
    public static final String LOCATION_OF_HOLDING = "locationOfHolding";
    public static final String MIN_FILE_QUEUE_SIZE = "minFileQueueSize";
    public static final String INGEST_SUMMARY_PERIOD_IN_MIN = "ingestSummaryPeriodInMin";

    public final static List<String> REQUIRED_PROPS = newArrayList(
        LOCATION_OF_RAW_DATA,
        LOCATION_OF_HOLDING,
        MIN_FILE_QUEUE_SIZE,
        INGEST_SUMMARY_PERIOD_IN_MIN
    );

    //OPTIONAL PROPERTIES AND THEIR DEFAULTS
    public static final String INGEST_SORT_DURATION_IN_SEC = "ingestSortDurationInSec";
    public static final long DEFAULT_INGEST_SORT_DURATION_IN_SEC = 120;
    public static final String INGEST_SUMMARY_DELAY_IN_MIN = "ingestSummaryDelayInMin";
    private static final long DEFAULT_INGEST_SUMMARY_DELAY_IN_MIN = 1;

    private final ImmutableConfig config;

    public FileIngestProps(Properties props) {
        this.config = new ImmutableConfig(props, REQUIRED_PROPS);
    }

    public String locationOfRawData() {
        return config.getString(LOCATION_OF_RAW_DATA);
    }

    /**
     * This is where raw NOP data is stored while we wait for an "ingestion thread" to process the
     * file.
     *
     * @return The path where raw NOP data is moved to while it is waiting to be processed.
     */
    public String locationOfHolding() {
        return config.getString(LOCATION_OF_HOLDING);
    }

    public int minFileQueueSize() {
        return config.getInt(MIN_FILE_QUEUE_SIZE);
    }

    /**
     * This option governs how much Point data is buffered and sorted before being forwarded to the
     * "ingest destination" (ie the target Kafka Point Topic or other Point Consumer). This option
     * is not required and only relevant when the "sortOnIngest" property is True
     *
     * @return The number of seconds worth of data to buffer.
     */
    public Duration ingestSortDurationInSec() {
        long numSec = config.getOptionalLong(INGEST_SORT_DURATION_IN_SEC)
            .orElse(DEFAULT_INGEST_SORT_DURATION_IN_SEC);

        return Duration.ofSeconds(numSec);
    }

    public long ingestSummaryPeriod() {
        return config.getLong(INGEST_SUMMARY_PERIOD_IN_MIN);
    }

    public long ingestSummaryDelay() {
        return config.getOptionalLong(INGEST_SUMMARY_DELAY_IN_MIN)
            .orElse(DEFAULT_INGEST_SUMMARY_DELAY_IN_MIN);
    }
}
