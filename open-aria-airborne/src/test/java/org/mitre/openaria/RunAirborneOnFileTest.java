package org.mitre.openaria;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RunAirborneOnFileTest {

    @Disabled
    @Test
    public void mainMethodProcessesOneFile() throws IOException {
        // This test tasks about 10 minutes to run (it processes, 1.2M RH messages)
        // Ideally it would be classified as a "large test" and be run less often (e.g. pre-PR but not in the main-development loop)

        String[] args = new String[]{
            "-c", "src/test/resources/processOneFile.yaml",
            "-f", "/users/jiparker/rawData/data-from-2021-05-30/STARS_D10_RH_20210530.txt.gz"
        };

        assertDoesNotThrow(
            () -> RunAirborneOnFile.main(args)
        );

        File targetAvroFile = new File("airborneEvents.avro");
        if (targetAvroFile.exists()) {
            targetAvroFile.delete();
        }

        File eventDir = new File("myEventsGoHere");
        Files.deleteIfExists(eventDir.toPath());
        // //Uses org.apache.commons.io.FileUtils
        // if (eventDir.exists()) {
        //     FileUtils.deleteDirectory(eventDir);
        // }
    }



    @Test
    public void runProjectDemo() throws IOException {
        // Verify the "detected-encounters.md" demo works

        String[] args = new String[]{
            "-c", "src/test/resources/sampleConfig.yaml",
            "-f", "src/test/resources/sampleData.txt.gz"
        };

        assertDoesNotThrow(
            () -> RunAirborneOnFile.main(args)
        );

        File eventDir = new File("detectedEvents");
        File event1 = new File(eventDir, "2018-03-24--D21--1200--1200--54226.json");
        File event2 = new File(eventDir, "2018-03-24--D21--N518SP--1200--54180.json");
        File event3 = new File(eventDir, "2018-03-24--D21--N518SP--1200--54345.json");

        event1.delete();
        event2.delete();
        event3.delete();

        Files.deleteIfExists(eventDir.toPath());
    }


    @Test
    public void runProjectDemo_aggregateEvents() throws IOException {
        // Verify the "aggregate-encounters.md" demo works

        String[] args = new String[]{
            "-c", "src/test/resources/sampleConfig2.yaml",
            "-f", "src/test/resources/sampleData.txt.gz"
        };

        assertDoesNotThrow(
            () -> RunAirborneOnFile.main(args)
        );

        File eventDir = new File("detectedEvents");
        File event1 = new File(eventDir, "2018-03-24--D21--1200--1200--54226.json");
        File event2 = new File(eventDir, "2018-03-24--D21--N518SP--1200--54180.json");
        File event3 = new File(eventDir, "2018-03-24--D21--N518SP--1200--54345.json");

        event1.delete();
        event2.delete();
        event3.delete();

        Files.deleteIfExists(eventDir.toPath());

        File targetAvroFile = new File("allEvents.avro");
        if (targetAvroFile.exists()) {
            targetAvroFile.delete();
        }
    }
}