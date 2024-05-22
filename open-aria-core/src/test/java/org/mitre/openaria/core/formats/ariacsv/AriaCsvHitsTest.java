package org.mitre.openaria.core.formats.ariacsv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.openaria.core.Point;

import org.junit.jupiter.api.Test;

class AriaCsvHitsTest {


    @Test
    public void exampleParsing_noExtraData() {

        String rawCsv = ",,2018-03-24T14:41:09.371Z,vehicleIdNumber,42.9525,-83.7056,2700";

        Point<AriaCsvHit> pt = AriaCsvHits.parsePointFromAriaCsv(rawCsv);

        assertThat(pt.time(), is(Instant.parse("2018-03-24T14:41:09.371Z")));
        assertThat(pt.trackId(), is("vehicleIdNumber"));
        assertThat(pt.latitude(), is(42.9525));
        assertThat(pt.longitude(), is(-83.7056));
        assertThat(pt.altitude(), is(Distance.ofFeet(2700)));
        assertThat(pt.velocity(), nullValue());
        assertThat("The entire rawCsv text is accessible from the parsed point", pt.rawData().rawCsvText(), is(rawCsv));
    }

    @Test
    public void exampleParsing_failCorrectlyWhenOutOfBounds() {

        String rawCsv = ",,2018-03-24T14:41:09.371Z,vehicleIdNumber,42.9525,-83.7056,2700";

        Point<AriaCsvHit> pt = AriaCsvHits.parsePointFromAriaCsv(rawCsv);

        assertThat("The entire rawCsv text is accessible from the parsed point", pt.rawData().rawCsvText(), is(rawCsv));

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> pt.rawData().token(7));
    }

    @Test
    public void exampleParsing_withExtraData() {

        String rawCsv = "PRIMARY_PARTITION,SECONDARY_PARTITION,2018-03-24T14:41:09.371Z,vehicleIdNumber,42.9525,-83.7056,2700,EXTRA_FIELD_A,EXTRA_FIELD_B";

        Point<AriaCsvHit> pt = AriaCsvHits.parsePointFromAriaCsv(rawCsv);

        assertThat(pt.time(), is(Instant.parse("2018-03-24T14:41:09.371Z")));
        assertThat(pt.trackId(), is("vehicleIdNumber"));
        assertThat(pt.latitude(), is(42.9525));
        assertThat(pt.longitude(), is(-83.7056));
        assertThat(pt.altitude(), is(Distance.ofFeet(2700)));
        assertThat(pt.velocity(), nullValue());
        assertThat("The entire rawCsv text is accessible from the parsed point", pt.rawData().rawCsvText(), is(rawCsv));

        assertThat(pt.rawData().token(0), is("PRIMARY_PARTITION"));
        assertThat(pt.rawData().token(1), is("SECONDARY_PARTITION"));
        assertThat(pt.rawData().token(7), is("EXTRA_FIELD_A"));
        assertThat(pt.rawData().token(8), is("EXTRA_FIELD_B"));
    }


    @Test
    public void exampleParsing_withExtraData_AND_withTrailingComma() {

        String rawCsv = "PRIMARY_PARTITION,SECONDARY_PARTITION,2018-03-24T14:41:09.371Z,vehicleIdNumber,42.9525,-83.7056,2700,EXTRA_FIELD_A,EXTRA_FIELD_B,";

        Point<AriaCsvHit> pt = AriaCsvHits.parsePointFromAriaCsv(rawCsv);

        assertThat(pt.time(), is(Instant.parse("2018-03-24T14:41:09.371Z")));
        assertThat(pt.trackId(), is("vehicleIdNumber"));
        assertThat(pt.latitude(), is(42.9525));
        assertThat(pt.longitude(), is(-83.7056));
        assertThat(pt.altitude(), is(Distance.ofFeet(2700)));
        assertThat(pt.velocity(), nullValue());
        assertThat("The entire rawCsv text is accessible from the parsed point", pt.rawData().rawCsvText(), is(rawCsv));

        assertThat(pt.rawData().token(0), is("PRIMARY_PARTITION"));
        assertThat(pt.rawData().token(1), is("SECONDARY_PARTITION"));
        assertThat(pt.rawData().token(7), is("EXTRA_FIELD_A"));
        assertThat(pt.rawData().token(8), is("EXTRA_FIELD_B"));
    }

}