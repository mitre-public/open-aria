package org.mitre.openaria;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class JCommanderTest {

    /** Use JCommander command line argument parser utility to create instances of this class. */
    private static class CliArgs {

        @Parameter(names = {"-c", "--config"}, required = true, description = "A yaml config file")
        String configFileArg;

        //JCommander builds this list of unflagged arguments by combining all Command Line args that don't have a flag like -c or --config
        @Parameter
        List<String> flaglessArgs;
    }


    @Test
    public void useJCommanderToExtractConfigAndFileList_cfgAtStart() {

        final String CONFIG_FILE_NAME = "src/test/resources/config.yaml";
        final String FILE_1 = "file1.gz";
        final String FILE_2 = "file2.gz";
        final String FILE_3 = "file3.gz";

        //Make String[] argv you'd get from running:
        // >>> java -cp org.MainMethod -c CONFIG_FILE FILE_1 FILE_2 FILE_3
        String[] manualArgs = new String[]{
            "-c", CONFIG_FILE_NAME, FILE_1, FILE_2, FILE_3
        };

        //Use JCommander to parse some simple CLI args into a useful class
        CliArgs parsedArgs = parseCommandLineArgs(manualArgs);

        assertThat(parsedArgs.configFileArg, is(CONFIG_FILE_NAME));
        assertThat(parsedArgs.flaglessArgs, hasSize(3));
        assertThat(parsedArgs.flaglessArgs.get(0), is(FILE_1));
        assertThat(parsedArgs.flaglessArgs.get(1), is(FILE_2));
        assertThat(parsedArgs.flaglessArgs.get(2), is(FILE_3));
    }


    @Test
    public void useJCommanderToExtractConfigAndFileList_cfgAtEnd() {

        final String CONFIG_FILE_NAME = "src/test/resources/config.yaml";
        final String FILE_1 = "file1.gz";
        final String FILE_2 = "file2.gz";
        final String FILE_3 = "file3.gz";

        //Make String[] argv you'd get from running:
        // >>> java -cp org.MainMethod FILE_1 FILE_2 FILE_3 -c CONFIG_FILE
        String[] manualArgs = new String[]{
            FILE_1, FILE_2, FILE_3, "-c", CONFIG_FILE_NAME
        };

        //Use JCommander to parse some simple CLI args into a useful class
        CliArgs parsedArgs = parseCommandLineArgs(manualArgs);

        assertThat(parsedArgs.configFileArg, is(CONFIG_FILE_NAME));
        assertThat(parsedArgs.flaglessArgs, hasSize(3));
        assertThat(parsedArgs.flaglessArgs.get(0), is(FILE_1));
        assertThat(parsedArgs.flaglessArgs.get(1), is(FILE_2));
        assertThat(parsedArgs.flaglessArgs.get(2), is(FILE_3));
    }

    @Test
    public void useJCommanderToExtractConfigAndFileList_cfgInMiddle() {

        final String CONFIG_FILE_NAME = "src/test/resources/config.yaml";
        final String FILE_1 = "file1.gz";
        final String FILE_2 = "file2.gz";
        final String FILE_3 = "file3.gz";

        //Make String[] argv you'd get from running:
        // >>> java -cp org.MainMethod FILE_1 -c CONFIG_FILE FILE_2 FILE_3
        String[] manualArgs = new String[]{
            FILE_1, "-c", CONFIG_FILE_NAME, FILE_2, FILE_3
        };

        //Use JCommander to parse some simple CLI args into a useful class
        CliArgs parsedArgs = parseCommandLineArgs(manualArgs);

        assertThat(parsedArgs.configFileArg, is(CONFIG_FILE_NAME));
        assertThat(parsedArgs.flaglessArgs, hasSize(3));
        assertThat(parsedArgs.flaglessArgs.get(0), is(FILE_1));
        assertThat(parsedArgs.flaglessArgs.get(1), is(FILE_2));
        assertThat(parsedArgs.flaglessArgs.get(2), is(FILE_3));
    }

    /* Use JCommander util to parse the command line args */
    private static CliArgs parseCommandLineArgs(String[] args) {

        CliArgs parsedArgs = new CliArgs();
        JCommander.newBuilder()
            .addObject(parsedArgs)
            .build()
            .parse(args);

        return parsedArgs;
    }
}