package org.mitre.openaria.core.temp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import org.mitre.openaria.core.NopPoint;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvHit;

import org.junit.jupiter.api.Test;

class NopToCsvTest {


    public static final String CENTER_RH_MESSAGE = "[RH],Center,ZLA_B,07-10-2016,06:16:35.000,SKW5840,CRJ2,L,4712,110,355,124,33.4922,-118.1300,465,,,,,/,,ZLA_B,,,,D0608,SAN,,IFR,,465,1396392357,LAX,,110//110,,L,1,,,{RH}";
    public static final String STARS_RH_MESSAGE = "[RH],STARS,A80_B,07/10/2016,20:03:53.856,DAL200,MD88,D,1311,159,339,221,034.27719,-083.63591,1519,1311,57.2078,66.6181,1,L,A,A80,,DRE,ATL,2006,ATL,ACT,IFR,,01465,,,,,27L,L,1,,0,{RH}";
    public static final String MEARTS_RH_MESSAGE = "[RH],MEARTS,ZUA_B,11-05-2019,15:28:06.020,UAL185,B737,L,2646,400,450,239,011.6384,141.6778,257,,67.50287,145.9169,,ZUA/1F,,ZUA_B,,,,,,,,,,,,E1430,400//400,,L,1,{RH}";

    @Test
    public void canTranslateCenter() {

        NopPoint centerPoint = NopPoint.from(CENTER_RH_MESSAGE);
        String csvString = NopToCsv.nopToAriaCsvFormat(CENTER_RH_MESSAGE);

        AriaCsvHit pt = new AriaCsvHit(csvString);

        assertThat(pt.time(), is(centerPoint.time()));
        assertThat(pt.latitude(), is(centerPoint.latitude()));
        assertThat(pt.longitude(), is(centerPoint.longitude()));
        assertThat(pt.linkId(), is("ZLA-465"));
        assertThat(pt.altitude(), is(centerPoint.altitude()));
        assertThat(pt.rawCsvText(), is(",,2016-07-10T06:16:35Z,ZLA-465,33.4922,-118.1300,11000,W1JIXSxDZW50ZXIsWkxBX0IsMDctMTAtMjAxNiwwNjoxNjozNS4wMDAsU0tXNTg0MCxDUkoyLEwsNDcxMiwxMTAsMzU1LDEyNCwzMy40OTIyLC0xMTguMTMwMCw0NjUsLCwsLC8sLFpMQV9CLCwsLEQwNjA4LFNBTiwsSUZSLCw0NjUsMTM5NjM5MjM1NyxMQVgsLDExMC8vMTEwLCxMLDEsLCx7Ukh9"));
    }

    @Test
    public void canTranslateStars() {

        NopPoint starsPoint = NopPoint.from(STARS_RH_MESSAGE);
        String csvString = NopToCsv.nopToAriaCsvFormat(STARS_RH_MESSAGE);
        AriaCsvHit pt = new AriaCsvHit(csvString);

        assertThat(pt.time(), is(starsPoint.time()));
        assertThat(pt.latitude(), closeTo(starsPoint.latitude(), 0.0001));
        assertThat(pt.longitude(), closeTo(starsPoint.longitude(), 0.0001));
        assertThat(pt.linkId(), is("A80-1519"));
        assertThat(pt.altitude(), is(starsPoint.altitude()));
        assertThat(pt.rawCsvText(), is(",,2016-07-10T20:03:53.856Z,A80-1519,34.2772,-83.6359,15900,W1JIXSxTVEFSUyxBODBfQiwwNy8xMC8yMDE2LDIwOjAzOjUzLjg1NixEQUwyMDAsTUQ4OCxELDEzMTEsMTU5LDMzOSwyMjEsMDM0LjI3NzE5LC0wODMuNjM1OTEsMTUxOSwxMzExLDU3LjIwNzgsNjYuNjE4MSwxLEwsQSxBODAsLERSRSxBVEwsMjAwNixBVEwsQUNULElGUiwsMDE0NjUsLCwsLDI3TCxMLDEsLDAse1JIfQ"));
    }

    @Test
    public void canTranslateMearts() {

        NopPoint meartsPoint = NopPoint.from(MEARTS_RH_MESSAGE);
        String csvString = NopToCsv.nopToAriaCsvFormat(MEARTS_RH_MESSAGE);
        AriaCsvHit pt = new AriaCsvHit(csvString);

        assertThat(pt.time(), is(meartsPoint.time()));
        assertThat(pt.latitude(), closeTo(meartsPoint.latitude(), 0.0001));
        assertThat(pt.longitude(), closeTo(meartsPoint.longitude(), 0.0001));
        assertThat(pt.linkId(), is("ZUA-257"));
        assertThat(pt.altitude(), is(meartsPoint.altitude()));
        assertThat(pt.rawCsvText(), is(",,2019-11-05T15:28:06.020Z,ZUA-257,11.6384,141.6778,40000,W1JIXSxNRUFSVFMsWlVBX0IsMTEtMDUtMjAxOSwxNToyODowNi4wMjAsVUFMMTg1LEI3MzcsTCwyNjQ2LDQwMCw0NTAsMjM5LDAxMS42Mzg0LDE0MS42Nzc4LDI1NywsNjcuNTAyODcsMTQ1LjkxNjksLFpVQS8xRiwsWlVBX0IsLCwsLCwsLCwsLCxFMTQzMCw0MDAvLzQwMCwsTCwxLHtSSH0"));
    }

}