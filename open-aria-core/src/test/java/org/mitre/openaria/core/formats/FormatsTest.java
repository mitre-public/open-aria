package org.mitre.openaria.core.formats;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvHit;
import org.mitre.openaria.core.formats.nop.NopHit;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

class FormatsTest {

    @Test
    void supportsNop() {
        assertDoesNotThrow(() -> Formats.getFormat("nop"));
        assertDoesNotThrow(() -> Formats.getFormat(" nop "));
        assertDoesNotThrow(() -> Formats.getFormat("NOP"));
        assertDoesNotThrow(() -> Formats.getFormat(" NOP "));
    }

    @Test
    void supportsCsv() {
        assertDoesNotThrow(() -> Formats.getFormat("csv"));
        assertDoesNotThrow(() -> Formats.getFormat(" csv "));
        assertDoesNotThrow(() -> Formats.getFormat("CSV"));
        assertDoesNotThrow(() -> Formats.getFormat(" CSV "));
    }

    @Test
    void doesNotSupportAdsb() {
        // Hopefully we will support ADSB at some point in the future ...

        assertThrows(IllegalArgumentException.class, () -> Formats.getFormat("adsb"));
        assertThrows(IllegalArgumentException.class, () -> Formats.getFormat(" adsb "));
        assertThrows(IllegalArgumentException.class, () -> Formats.getFormat("ADSB"));
        assertThrows(IllegalArgumentException.class, () -> Formats.getFormat(" ADSB "));
    }

    @Test
    void nopFormatCanParseNop() {
        File nopData = new File("src/test/resources/nop/STARS_SAMPLE.txt.gz");

        @SuppressWarnings("unchecked")
        Format<NopHit> format = (Format<NopHit>) Formats.getFormat("nop");

        Iterator<Point<NopHit>> iter = format.parseFile(nopData);

        String example1 = "[RH],STARS,A80_B,07/10/2016,12:50:15.432,N449JL,DA40,F,7230,080,121,292,032.95559,-082.05560,0306,7230,137.8640,-11.0694,0,C,A,A80,,JRP,PUJ,1312,PUJ,ACT,IFR,,01671,,,,,,S,1,,0,{RH}";
        String example2 = "[RH],STARS,A80_B,07/10/2016,12:50:15.502,,,,4024,340,463,183,033.46947,-082.33134,2715,0000,123.2234,19.3642,,,,A80,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";
        String example3 = "[RH],STARS,A80_B,07/10/2016,12:50:15.591,SWA3109,B737,D,4064,012,134,270,033.64943,-084.39433,0014,4064,19.6336,28.7353,1,V,A,A80,VR,DRE,ATL,1240,ATL,ACT,IFR,,01526,,,,,26R,L,1,,0,{RH}";

        List<Point<NopHit>> data = Lists.newArrayList(iter);

        List<String> dataAsStrings = data.stream()
            .map(pt -> format.asRawString(pt.rawData()))
            .toList();

        assertThat(data, hasSize(12593));
        assertThat(dataAsStrings.contains(example1), is(true));
        assertThat(dataAsStrings.contains(example2), is(true));
        assertThat(dataAsStrings.contains(example3), is(true));
    }


    @Test
    void csvFormatCanParseCsv() {
        File csvData = new File("src/test/resources/openariacsv/scaryTrackData_openAriaCsv.txt");

        @SuppressWarnings("unchecked")
        Format<AriaCsvHit> format = (Format<AriaCsvHit>) Formats.getFormat("csv");

        Iterator<Point<AriaCsvHit>> iter = format.parseFile(csvData);

        String example1 = ",,2018-03-24T14:41:46.290Z,D21-3472,42.9343,-83.7085,3100,W1JIXSxTVEFSUyxEMjFfQiwwMy8yNC8yMDE4LDE0OjQxOjQ2LjI5MCxONTE4U1AsQzE3MiwsNTI1NiwwMzEsMTA1LDE4NCwwNDIuOTM0MzQsLTA4My43MDg1NCwzNDcyLDUyNTYsLTE0LjUxODMsNDMuMjkwMiwxLFksQSxEMjEsLFBPTCxBUkIsMTQ0NixBUkIsQUNULFZGUiwsMDE1MDAsLCwsLCxTLDEsLDAse1JIfQ";
        String example2 = ",,2018-03-24T14:41:50.900Z,D21-3472,42.9319,-83.7088,3100,W1JIXSxTVEFSUyxEMjFfQiwwMy8yNC8yMDE4LDE0OjQxOjUwLjkwMCxONTE4U1AsQzE3MiwsNTI1NiwwMzEsMTA2LDE4MywwNDIuOTMxOTMsLTA4My43MDg3OSwzNDcyLDUyNTYsLTE0LjUzMDAsNDMuMTQ1NywxLFksQSxEMjEsLFBPTCxBUkIsMTQ0NixBUkIsQUNULFZGUiwsMDE1MDAsLCwsLCxTLDEsLDAse1JIfQ";
        String example3 = ",,2018-03-24T14:41:55.510Z,D21-3472,42.9295,-83.7091,3100,W1JIXSxTVEFSUyxEMjFfQiwwMy8yNC8yMDE4LDE0OjQxOjU1LjUxMCxONTE4U1AsQzE3MiwsNTI1NiwwMzEsMTA3LDE4MywwNDIuOTI5NTIsLTA4My43MDkxNCwzNDcyLDUyNTYsLTE0LjU0NTcsNDMuMDAxMSwxLFksQSxEMjEsLFBPTCxBUkIsMTQ0NixBUkIsQUNULFZGUiwsMDE1MDAsLCwsLCxTLDEsLDAse1JIfQ";

        List<Point<AriaCsvHit>> data = Lists.newArrayList(iter);

        List<String> dataAsStrings = data.stream()
            .map(pt -> format.asRawString(pt.rawData()))
            .toList();

        assertThat(data, hasSize(520));
        assertThat(dataAsStrings.contains(example1), is(true));
        assertThat(dataAsStrings.contains(example2), is(true));
        assertThat(dataAsStrings.contains(example3), is(true));
    }
}