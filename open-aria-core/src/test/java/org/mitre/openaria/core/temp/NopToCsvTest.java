package org.mitre.openaria.core.temp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.PositionRecord;
import org.mitre.openaria.core.CsvPositioner;

class NopToCsvTest {


    public static final String CENTER_RH_MESSAGE = "[RH],Center,ZLA_B,07-10-2016,06:16:35.000,SKW5840,CRJ2,L,4712,110,355,124,33.4922,-118.1300,465,,,,,/,,ZLA_B,,,,D0608,SAN,,IFR,,465,1396392357,LAX,,110//110,,L,1,,,{RH}";
    public static final String STARS_RH_MESSAGE = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-083.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
    public static final String MEARTS_RH_MESSAGE = "[RH],MEARTS,ZUA_B,11-05-2019,15:28:06.020,UAL185,B737,L,2646,400,450,239,011.6384,141.6778,257,,67.50287,145.9169,,ZUA/1F,,ZUA_B,,,,,,,,,,,,E1430,400//400,,L,1,{RH}";


    private static final CsvPositioner positioner = new CsvPositioner();

    @Test
    public void canTranslateCenter() {

        String translated = NopToCsv.nopToGenericCsv(CENTER_RH_MESSAGE);

        PositionRecord<String> position = positioner.asRecord(translated);

        String expectedDate = "2016-07-10";
        String expectedTime = "06:16:35.000";
        assertThat(translated.contains(expectedDate + "," + expectedTime), is(true));
        Instant time = CsvPositioner.parseTime(expectedDate,expectedTime);

        assertThat(position.time(), is(time));
        assertThat(position.latitude(), closeTo(33.4922, 0.00001));
        assertThat(position.longitude(), closeTo(-118.1300, 0.00001));
        assertThat(position.altitude().inFeet(), closeTo(11000.0, 0.001));

        assertThat(position.datum(), is(translated));
    }


    @Test
    public void canTranslateStars() {

        String translated = NopToCsv.nopToGenericCsv(STARS_RH_MESSAGE);

        PositionRecord<String> position = positioner.asRecord(translated);

        String expectedDate = "2016-07-10";
        String expectedTime = "20:03:53.856";
        assertThat(translated.contains(expectedDate + "," + expectedTime), is(true));
        Instant time = CsvPositioner.parseTime(expectedDate,expectedTime);

        assertThat(position.time(), is(time));
        assertThat(position.latitude(), closeTo(34.27719, 0.00001));
        assertThat(position.longitude(), closeTo(-83.63591, 0.00001));
        assertThat(position.altitude().inFeet(), closeTo(15900.0, 0.001));

        assertThat(position.datum(), is(translated));
    }


    @Test
    public void canTranslateMearts() {

        String translated = NopToCsv.nopToGenericCsv(MEARTS_RH_MESSAGE);

        PositionRecord<String> position = positioner.asRecord(translated);

        String expectedDate = "2019-11-05";
        String expectedTime = "15:28:06.020";
        assertThat(translated.contains(expectedDate + "," + expectedTime), is(true));
        Instant time = CsvPositioner.parseTime(expectedDate,expectedTime);

        assertThat(position.time(), is(time));
        assertThat(position.latitude(), closeTo(11.6384, 0.00001));
        assertThat(position.longitude(), closeTo(141.6778, 0.00001));
        assertThat(position.altitude().inFeet(), closeTo(40000.0, 0.001));

        assertThat(position.datum(), is(translated));
    }

}