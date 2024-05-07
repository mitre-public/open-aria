package org.mitre.openaria.core.formats.nop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;

public class NopParserTest {

    String AGW_FILE = System.getProperty("user.dir") + "/src/test/resources/nop/AGW_SAMPLE.txt.gz";
    String STARS_FILE = System.getProperty("user.dir") + "/src/test/resources/nop/STARS_SAMPLE.txt.gz";
    String CENTER_FILE = System.getProperty("user.dir") + "/src/test/resources/nop/CENTER_SAMPLE.txt.gz";
    String MEARTS_FILE = System.getProperty("user.dir") + "/src/test/resources/nop/MEARTS_SAMPLE.txt.gz";

    String FILE_WITH_BAD_LINE = System.getProperty("user.dir") + "/src/test/resources/nop/NopFileWithBadLine.txt";

    String FILE_WITH_LOTS_OF_BAD_LINES = System.getProperty("user.dir") + "/src/test/resources/nop/flawDataThatCanBreakParser.txt";

    @Test
    public void testAgwParsing() {

        NopParser parser = new NopParser(new File(AGW_FILE));

        parseAllMessages(parser);

        assertThat(parser.currentLineCount(), is(5542));
        assertThat(parser.exceptionCount(), is(0));
    }

    @Test
    public void testStarsParsing() {

        NopParser parser = new NopParser(new File(STARS_FILE));

        assertDoesNotThrow(() -> parseAllMessages(parser));

        System.out.println(parser.currentLineCount());
        assertThat(parser.currentLineCount(), is(12667));
        assertThat(parser.exceptionCount(), is(0));
    }

    @Test
    public void testCenterParsing() {

        NopParser parser = new NopParser(new File(CENTER_FILE));

        parseAllMessages(parser);

        assertThat(parser.currentLineCount(), is(11632));
        assertThat(parser.exceptionCount(), is(0));
    }

    @Test
    public void testMeartsParsing() {

        NopParser parser = new NopParser(new File(MEARTS_FILE));

        assertDoesNotThrow(() -> parseAllMessages(parser));

        assertThat(parser.currentLineCount(), is(298));
        assertThat(parser.exceptionCount(), is(0));
    }

    @Test
    public void testFileWithFlaw() {

        NopParser parser = new NopParser(new File(FILE_WITH_BAD_LINE));

        assertDoesNotThrow(() -> parseAllMessages(parser));

        assertEquals(1, parser.exceptionCount());
        assertEquals(5, parser.currentLineCount());
    }

    @Test
    public void testFileWithManyFlaws() {

        NopParser parser = new NopParser(new File(FILE_WITH_LOTS_OF_BAD_LINES));

        assertDoesNotThrow(() -> parseAllMessages(parser));

        assertEquals(10000, parser.exceptionCount());
    }


    private void parseAllMessages(NopParser parser) {

        while (parser.hasNext()) {
            NopMessage next = parser.next();
        }
    }

    @Test
    public void testFixingBadMillisecondTimings() {
        //the millisecond value of this piece of raw NOP data is not formated correctly
        String RADAR_HIT_WITH_BAD_TIME = "[RH],STARS,A80,10/18/2016,00:57:121.00,,,,1200,014,099,049,033.00800,-082.74128,3717,0000,103.1843,-8.7140,,,,A80,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";
        String RADAR_HIT_WITH_GOOD_TIME = "[RH],STARS,A80,10/18/2016,00:57:12.999,,,,1200,014,099,049,033.00800,-082.74128,3717,0000,103.1843,-8.7140,,,,A80,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}";

        StarsRadarHit resultFromBadTime = (StarsRadarHit) NopMessageType.parse(RADAR_HIT_WITH_BAD_TIME);
        StarsRadarHit resultFromGoodTime = (StarsRadarHit) NopMessageType.parse(RADAR_HIT_WITH_GOOD_TIME);

        assertEquals(
            resultFromGoodTime.time(),
            resultFromBadTime.time()
        );
    }

    @Test
    public void testMissingCourse() {

        String RADAR_HIT_WITH_NO_COURSE = "[RH],STARS,GEG,07/08/2017,14:09:11.474,,,,1200,0,0,,47.61734,-117.54339,655,0,0.3008,-0.1445,,,,GEG,,,,,,,IFR,,,,,,,,,,,,{RH}\n";

        StarsRadarHit radarHit = (StarsRadarHit) NopMessageType.parse(RADAR_HIT_WITH_NO_COURSE);

        assertEquals(
            null,
            radarHit.heading()
        );

    }
}
