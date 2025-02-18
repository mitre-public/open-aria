package org.mitre.openaria;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.InspectDataset.parseCommandLineArgs;

import java.io.File;

import org.mitre.openaria.InspectDataset.CommandLineArgs;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class InspectDatasetTest {

    @Test
    void demoNopAudit() {
        String[] args = new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--map"};

        assertDoesNotThrow(
            () -> InspectDataset.main(args)
        );

        File targetMapFile = new File("map-of-sampleNopData.txt.png");
        if (targetMapFile.exists()) {
            targetMapFile.delete();
        }
    }

    @Test
    void demoCsvAudit() {
        String[] args = new String[]{"-f", "src/main/resources/sampleCsvData.txt.gz", "--csv"};

        assertDoesNotThrow(
            () -> InspectDataset.main(args)
        );
    }


    @Disabled
    @Test
    void demoNopAudit_custom_color() {
        String[] args = new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--map", "--mapBoxTiles", "--green", "255", "--alpha", "35"};

        assertDoesNotThrow(
            () -> InspectDataset.main(args)
        );

        File targetMapFile = new File("map-of-sampleNopData.txt.png");
        if (targetMapFile.exists()) {
            targetMapFile.delete();
        }
    }


    @Test
    void cliParsesMapFlag() {

        // Note: the -f file flag is required and the corresponding file must be real
        String[] argsWith = new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--csv", "--map"};
        String[] argsWithout = new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--csv"};

        CommandLineArgs cliArgsWith = parseCommandLineArgs(argsWith);
        CommandLineArgs cliArgsWithout = parseCommandLineArgs(argsWithout);

        assertThat(cliArgsWith.shouldDrawMap, is(true));
        assertThat(cliArgsWithout.shouldDrawMap, is(false));
    }

    @Test
    void cliParsesCsv() {

        // Note: the -f file flag is required and the corresponding file must be real
        String[] args = new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--csv"};

        CommandLineArgs cliArgsWith = parseCommandLineArgs(args);

        assertThat(cliArgsWith.parseCsv, is(true));
        assertThat(cliArgsWith.parseNop, is(false));
    }

    @Test
    void cliParsesNop() {

        // Note: the -f file flag is required and the corresponding file must be real
        String[] args = new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop"};

        CommandLineArgs cliArgsWith = parseCommandLineArgs(args);

        assertThat(cliArgsWith.parseNop, is(true));
        assertThat(cliArgsWith.parseCsv, is(false));
    }

    @Test
    void cliPreventsNopAndCsv() {

        // Note: the -f file flag is required and the corresponding file must be real
        String[] args = new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv"};

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(args)
        );
    }

    @Test
    void cliPreventsBadColors() {

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--red" , "-1"})
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--red" , "256"})
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--green" , "-1"})
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--green" , "256"})
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--blue" , "-1"})
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--blue" , "256"})
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--alpha" , "0"})
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(new String[]{"-f", "src/main/resources/sampleNopData.txt.gz", "--nop", "--csv", "--alpha" , "256"})
        );
    }
}