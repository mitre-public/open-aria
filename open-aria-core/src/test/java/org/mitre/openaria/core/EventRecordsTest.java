
package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mitre.openaria.core.EventRecords.*;

import java.time.Instant;
import java.util.Properties;

import org.mitre.caasd.commons.util.PropertyUtils;

import org.junit.jupiter.api.Test;


public class EventRecordsTest {

    /* How EPOCH time should appear in a EventRecord. */
    private static final String EPOCH_AS_STRING = "01/01/1970,00:00:00.000";

    /* How A time soon after EPOCH should appear in a EventRecord. */
    private static final String SECOND_EXAMPLE_STRING = "01/01/1970,13:02:10.043";

    /* The Instant that corresponds to the above example. */
    private static final Instant SECOND_EXAMPLE_INSTANT = EPOCH
        .plusMillis(13 * 1000 * 60 * 60) //hours
        .plusMillis(2 * 1000 * 60) //minutes
        .plusMillis(10 * 1000) //seconds
        .plusMillis(43); //milliseconds

    @Test
    public void testEpochStringCanGoIntoPropetyFiles() {
        String fileContent = new StringBuilder()
            .append("prop1 : 22\n")
            .append("time1 : " + EPOCH_AS_STRING + "\n")
            .append("prop2 : aPropertyValue\n")
            .append("time2 : " + SECOND_EXAMPLE_STRING + "\n")
            .toString();

        Properties props = PropertyUtils.parseProperties(fileContent);
        assertEquals(4, props.size());

        String value1 = props.getProperty("time1");
        String value2 = props.getProperty("time2");

        assertEquals(EPOCH_AS_STRING, value1);
        assertEquals(SECOND_EXAMPLE_STRING, value2);
        assertEquals("22", props.getProperty("prop1"));
        assertEquals("aPropertyValue", props.getProperty("prop2"));
    }

    @Test
    public void testParsingTime() {
        assertEquals(EPOCH, parseTime(EPOCH_AS_STRING));
        assertEquals(SECOND_EXAMPLE_INSTANT, parseTime(SECOND_EXAMPLE_STRING));
    }

    @Test
    public void safeBeaconGeneratesDefaultValue() {

        Point p = Point.builder()
            .time(EPOCH)
            .latLong(0.0, 0.0)
            .build();

        assertThat(safeBeaconCode(p), is(UNKOWN_BEACON_VALUE));
    }
}
