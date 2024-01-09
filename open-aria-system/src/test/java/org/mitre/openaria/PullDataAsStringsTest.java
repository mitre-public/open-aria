package org.mitre.openaria;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.PullKafkaDataAsStrings.parseCommandLineArgs;

import org.junit.jupiter.api.Test;

class PullDataAsStringsTest {

    @Test
    public void validateArgumentParsing_simpleCase() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml"
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.topic, is("someTopic"));
        assertThat(args.partitionNum, is(5));
        assertThat(args.kafkaConfigFile, is("aConfigFile.yml"));
        assertThat(args.pullAllPartitions, is(false));
        assertThat(args.limitingMessageCount(), is(false));
    }


    @Test
    public void validateArgumentParsing_simpleCase_withNumMessages() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
            "-n", "20"
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.topic, is("someTopic"));
        assertThat(args.partitionNum, is(5));
        assertThat(args.kafkaConfigFile, is("aConfigFile.yml"));
        assertThat(args.pullAllPartitions, is(false));
        assertThat(args.numMessages, is(20));
        assertThat(args.limitingMessageCount(), is(true));
    }


    @Test
    public void validateArgumentParsing_simpleCase_withNumMessages_broken() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
            "-n", "0"  //too small!
        };

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(strArgs)
        );
    }

    @Test
    public void validateArgumentParsing_processAll() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "--all",
            "-c", "aConfigFile.yml"
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.topic, is("someTopic"));
        assertThat(args.pullAllPartitions, is(true));
        assertThat(args.partitionNum, nullValue());
        assertThat(args.kafkaConfigFile, is("aConfigFile.yml"));
        assertThat(args.limitingMessageCount(), is(false));
    }

    @Test
    public void validateArgumentParsing_cannotSpecifyPartitionAndAll() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "--all",
            "-c", "aConfigFile.yml"
        };

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(strArgs)
        );
    }

    @Test
    public void validateArgumentParsing_cannotWatchAndSetNumber() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
            "-n", "100",
            "--watch"  //cannot co-exist with "-n"
        };

        assertThrows(
            IllegalArgumentException.class,
            () -> parseCommandLineArgs(strArgs)
        );
    }

    @Test
    public void validateArgumentParsing_justWatching_happyPath() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
            "--watch"
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.topic, is("someTopic"));
        assertThat(args.partitionNum, is(5));
        assertThat(args.kafkaConfigFile, is("aConfigFile.yml"));
        assertThat(args.limitingMessageCount(), is(false));
        assertThat(args.justWatching, is(true));
    }

    @Test
    public void validateArgumentParsing_simpleCase_noOffsetsOrTimestamps() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.showOffsets, is(false));
        assertThat(args.showTimeStamps, is(false));
    }

    @Test
    public void validateArgumentParsing_simpleCase_withOffsets() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
            "--offsets"
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.showOffsets, is(true));
    }

    @Test
    public void validateArgumentParsing_simpleCase_withTimestamps() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
            "--timestamps"
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.showTimeStamps, is(true));
    }

    @Test
    public void validateArgumentParsing_simpleCase_WithOffsetsAndTimestamps() {

        String[] strArgs = new String[]{
            "-t", "someTopic",
            "-p", "5",
            "-c", "aConfigFile.yml",
            "--offsets",
            "--timestamps"
        };

        PullKafkaDataAsStrings.Args args = parseCommandLineArgs(strArgs);

        assertThat(args.showOffsets, is(true));
        assertThat(args.showTimeStamps, is(true));
    }
}