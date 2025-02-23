package org.mitre.openaria;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
            "-c", "src/main/resources/sampleNopConfig.yaml",
            "-f", "src/main/resources/sampleNopData.txt.gz"
        };

        assertDoesNotThrow(
            () -> RunAirborneOnFile.main(args)
        );

        File eventDir = new File("detectedEvents");
        assertThat(eventDir.exists(), is(true));

        File[] eventFiles = eventDir.listFiles();
        assertThat(eventFiles.length, is(1));

        Stream.of(eventFiles).forEach(file -> file.delete());
        Files.deleteIfExists(eventDir.toPath());

        File mapDir = new File("eventMaps");
        assertThat(mapDir.exists(), is(true));

        File[] mapFiles = mapDir.listFiles();
        assertThat(mapFiles.length, is(1));

        Stream.of(mapFiles).forEach(file -> file.delete());
        Files.deleteIfExists(mapDir.toPath());
    }


    @Test
    public void outputEventsContainSourceData_nop() throws IOException {
        // Verify using "publishTrackData = true" produces events with the track data in them

        String[] args = new String[]{
            "-c", "src/test/resources/sampleNopConfig-publishTracks.yaml",
            "-f", "src/main/resources/sampleNopData.txt.gz"
        };

        assertDoesNotThrow(() -> RunAirborneOnFile.main(args));

        // The output folder contains exactly 1 event --
        File eventDir = new File("nopEvents");
        assertThat(eventDir.exists(), is(true));
        File[] eventFiles = eventDir.listFiles();
        assertThat(eventFiles.length, is(1));

        // First and last radar hit for both aircraft involved.
        String[] requireText = new String[]{
            "[RH],STARS,D21,03/24/2018,14:55:42.389,N518SP,C172,,5256,032,108,183,042.50291,-083.75276,3472,5256,-16.5808,17.4191,1,Y,A,D21,,POL,ARB,1446,ARB,ACT,VFR,,01500,,,,,,S,1,,0,{RH}",
            "[RH],STARS,D21,03/24/2018,15:06:31.446,N518SP,C172,,5256,000,066,040,042.21653,-083.75817,3472,5256,-16.8972,0.2433,1,Y,A,D21,,POL,ARB,1446,ARB,CST,VFR,,01500,,,,,,S,1,,0,{RH}",
            "[RH],STARS,D21,03/24/2018,14:56:09.498,,,,1200,000,000,xxx,042.22761,-083.73510,2643,0000,-15.8660,0.9035,,,,D21,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}",
            "[RH],STARS,D21,03/24/2018,15:07:36.250,,,,1200,016,079,242,042.22036,-083.78027,2643,0000,-17.8816,0.4777,,,,D21,,,,,,ACT,IFR,,00000,,,,,,,1,,0,{RH}"
        };

        assertFileContains(eventFiles[0], requireText);

        Stream.of(eventFiles).forEach(file -> file.delete());
        Files.deleteIfExists(eventDir.toPath());
    }


    @Test
    public void outputEventsContainSourceData_csv() throws IOException {
        // Verify using "publishTrackData = true" produces events with the track data in them

        String[] args = new String[]{
            "-c", "src/test/resources/sampleCsvConfig-publishTracks.yaml",
            "-f", "src/main/resources/sampleCsvData.txt.gz"
        };

        assertDoesNotThrow(() -> RunAirborneOnFile.main(args));

        // The output folder contains exactly 1 event --
        File eventDir = new File("csvEvents");
        assertThat(eventDir.exists(), is(true));
        File[] eventFiles = eventDir.listFiles();
        assertThat(eventFiles.length, is(1));

        // First and last radar hit for both aircraft involved.
        String[] requireText = new String[]{
            ",,2018-03-24T14:47:55.471Z,D21-3472,42.7413,-83.7356,3100,W1JIXSxTVEFSUyxEMjEsMDMvMjQvMjAxOCwxNDo0Nzo1NS40NzEsTjUxOFNQLEMxNzIsLDUyNTYsMDMxLDExMSwxODcsMDQyLjc0MTMyLC0wODMuNzM1NjIsMzQ3Miw1MjU2LC0xNS43NjA1LDMxLjcxNjAsMSxZLEEsRDIxLCxQT0wsQVJCLDE0NDYsQVJCLEFDVCxWRlIsLDAxNTAwLCwsLCwsUywxLCwwLHtSSH0",
            ",,2018-03-24T15:06:31.446Z,D21-3472,42.2165,-83.7582,0,W1JIXSxTVEFSUyxEMjEsMDMvMjQvMjAxOCwxNTowNjozMS40NDYsLCwsNTI1NiwwMDAsMDY2LDA0MCwwNDIuMjE2NTMsLTA4My43NTgxNywzNDcyLDAwMDAsLTE2Ljg5NzIsMC4yNDMzLCwsLEQyMSwsLCwsLERSUCxJRlIsLDAwMDAwLCwsLCwsLDEsLDAse1JIfQ",
            ",,2018-03-24T14:56:09.498Z,D21-2643,42.2276,-83.7351,0,W1JIXSxTVEFSUyxEMjEsMDMvMjQvMjAxOCwxNDo1NjowOS40OTgsLCwsMTIwMCwwMDAsMDAwLHh4eCwwNDIuMjI3NjEsLTA4My43MzUxMCwyNjQzLDAwMDAsLTE1Ljg2NjAsMC45MDM1LCwsLEQyMSwsLCwsLEFDVCxJRlIsLDAwMDAwLCwsLCwsLDEsLDAse1JIfQ",
            ",,2018-03-24T15:08:13.219Z,D21-2643,42.2128,-83.7954,1700,W1JIXSxTVEFSUyxEMjEsMDMvMjQvMjAxOCwxNTowODoxMy4yMTksLCwsMTIwMCwwMTcsMDczLDIyOSwwNDIuMjEyODIsLTA4My43OTUzOCwyNjQzLDAwMDAsLTE4LjU1NzQsMC4wMjg1LCwsLEQyMSwsLCwsLEFDVCxJRlIsLDAwMDAwLCwsLCwsLDEsLDAse1JIfQ"
        };

        assertFileContains(eventFiles[0], requireText);

        Stream.of(eventFiles).forEach(file -> file.delete());
        Files.deleteIfExists(eventDir.toPath());
    }


    /** Verify that the given text file contains the following text. */
    public static void assertFileContains(File textFile, String[] requiredText) throws IOException {

        List<String> lines = Files.readAllLines(textFile.toPath());

        for (String requiredSnippet : requiredText) {
            long count = lines.stream().filter(str -> str.contains(requiredSnippet)).count();
            assertThat(requiredSnippet + " must exist in file", count >= 1, is(true));
        }
    }


    @Test
    public void runProjectDemo_aggregateEvents() throws IOException {
        // Verify the "aggregate-encounters.md" demo works

        String[] args = new String[]{
            "-c", "src/test/resources/sampleConfig2.yaml",
            "-f", "src/main/resources/sampleNopData.txt.gz"
        };

        assertDoesNotThrow(
            () -> RunAirborneOnFile.main(args)
        );

        File eventDir = new File("detectedEvents");
        File[] eventFiles = eventDir.listFiles();

        assertThat(eventFiles.length, is(1));

        Stream.of(eventFiles).forEach(file -> file.delete());
        Files.deleteIfExists(eventDir.toPath());

        File targetAvroFile = new File("allEvents.avro");
        if (targetAvroFile.exists()) {
            targetAvroFile.delete();
        }
    }
}