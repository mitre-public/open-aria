package org.mitre.openaria.core;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.openaria.core.CsvPositioner.*;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.PositionRecord;

class CsvPositionerTest {


    @Test
    public void defaultParsing_goodInput() {

        String sample1 = ",,1970-01-01,00:00:00.000,15.00,22.00,123, so much extra data we keep";

        CsvPositioner positioner = new CsvPositioner();

        PositionRecord<String> posRec = positioner.asRecord(sample1);

        assertThat(posRec.time(), is(EPOCH));
        assertThat(posRec.latitude(), is(15.0));
        assertThat(posRec.longitude(), is(22.0));
        assertThat(posRec.altitude(), is(Distance.ofFeet(123)));

        //Look at me!, The PositionRecord contains the whole input record!
        assertThat(posRec.datum(), is(sample1));
    }


    @Test
    public void defaultParsing_goodInput_withSlashes() {

        String sample1 = ",,1970/01/01,00:00:00.000,15.00,22.00,123, so much extra data we keep";

        CsvPositioner positioner = new CsvPositioner();

        PositionRecord<String> posRec = positioner.asRecord(sample1);

        assertThat(posRec.time(), is(EPOCH));
        assertThat(posRec.latitude(), is(15.0));
        assertThat(posRec.longitude(), is(22.0));
        assertThat(posRec.altitude(), is(Distance.ofFeet(123)));

        //Look at me!, The PositionRecord contains the whole input record!
        assertThat(posRec.datum(), is(sample1));
    }

    @Test
    public void formatTimeWorks() {
        assertThat(formatTime(EPOCH), is("1970-01-01,00:00:00.000"));
        assertThat(formatTime(EPOCH.plusMillis(2300)), is("1970-01-01,00:00:02.300"));
    }

}