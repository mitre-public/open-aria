
package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.openaria.core.formats.nop.NopMessage;
import org.mitre.openaria.core.formats.nop.NopMessageType;
import org.mitre.openaria.core.formats.nop.NopParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PointIteratorTest {

    static final String FIVE_LINES_OF_NOP_TEXT
        = """
        [RH],STARS,A80_B,07/10/2016,12:43:40.867,N732JE,C210,F,5634,053,158,146,032.35068,-084.54589,2478,5634,12.2195,-49.0616,1,Z,E,A80,PBI,O24,O12,,O12,ACT,VFR,,01278,,,,,,S,1,,0,{RH}
        [CA],STARS,A80_B,07/10/2016,12:43:40.868,28389,Continue,Mode_C_Intruder,2,NotSuppressed,3402,N6727N,033.98255,-084.02552,028,37.96,48.79,3536,,033.97386,-084.02184,028,38.15,48.27,{CA}
        [RH],STARS,A80_B,07/10/2016,12:43:40.868,,,,0000,000,148,138,032.72556,-082.78178,0950,0000,101.4617,-25.6671,,,,A80,,,,,,CST,IFR,,00000,,,,,,,1,,1,{RH}
        [Bytes]4951070{Bytes}
        [HB],7/12/2016 19:12:37,EOS:10,{HB}""";


    @TempDir
    public File tempDir;

    public File buildTestFile(String fileName) throws Exception {

        File testFile = new File(tempDir, fileName);
        FileUtils.appendToFile(testFile, FIVE_LINES_OF_NOP_TEXT);
        return testFile;
    }

    @Test
    public void testFileContents() throws Exception {

        int numConflictAlerts = 0;
        int numRadarHits = 0;
        int numHeartBeats = 0;
        int numBytes = 0;

        File testFile = buildTestFile("testNopFileA.txt");

        NopParser parser = new NopParser(testFile);

        while (parser.hasNext()) {
            NopMessage nopMessage = parser.next();

            if (nopMessage.getNopType() == NopMessageType.HEART_BEAT) {
                numHeartBeats++;
            }

            if (nopMessage.getNopType() == NopMessageType.CONFLICT_ALERT_MESSAGE) {
                numConflictAlerts++;
            }

            if (nopMessage.getNopType() == NopMessageType.BYTES_MESSAGE) {
                numBytes++;
            }

            if (nopMessage.getNopType().isRadarHit()) {
                numRadarHits++;
            }
        }

        assertEquals(1, numConflictAlerts);
        assertEquals(1, numBytes);
        assertEquals(1, numHeartBeats);
        assertEquals(2, numRadarHits);
    }

    @Test
    public void testNext() throws Exception {

        File testFile = buildTestFile("testNopFileB.txt");

        PointIterator iter = new PointIterator(new NopParser(testFile));

        int numPoints = 0;

        while (iter.hasNext()) {
            Point next = iter.next();

            if (numPoints == 0) {
                assertEquals(new LatLong(032.35068,-084.54589), next.latLong());
            }
            if (numPoints == 1) {
                assertEquals(LatLong.of(032.72556, -082.78178), next.latLong());
            }

            numPoints++;
        }

        assertEquals(2, numPoints);
    }
}
