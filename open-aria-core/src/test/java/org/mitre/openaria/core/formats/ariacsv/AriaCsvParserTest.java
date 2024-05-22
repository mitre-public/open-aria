package org.mitre.openaria.core.formats.ariacsv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.List;

import org.mitre.openaria.core.Point;

import org.junit.jupiter.api.Test;

class AriaCsvParserTest {


    @Test
    public void canParseFileOfData() {

        File testFile = new File("src/test/resources/openariacsv/scaryTrackData_openAriaCsv.txt");

        AriaCsvParser parser = new AriaCsvParser(testFile);

        List<Point<AriaCsvHit>> allPoints = parser.stream().toList();

        assertThat(allPoints.size(), is(520));
        assertThat(allPoints.stream().filter(p -> p.trackId().equals("D21-3472")).count(), is(338L));
        assertThat(allPoints.stream().filter(p -> p.trackId().equals("D21-2643")).count(), is(182L));
    }

}