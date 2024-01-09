package org.mitre.openaria.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.junit.jupiter.api.Test;

public class KafkaIngestorTest {

    @Test
    public void canBuildOptionsFromYaml() throws Exception {

        //load the yaml file that encodes a KafkaIngestor.Options
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("kafkaIngestorOptions.yaml").getFile());

        /* This Factory method reads the yaml file (which overrides values from the default constructor) */
        KafkaIngestor.Options options = KafkaIngestor.optionsFromYaml(yamlFile);

        assertThat(options.pointPrefetchLimit, is(500001));
        assertThat(options.numWorkerThreads, is(5));
        assertThat(options.milliSecBtwPollAttempts, is(2001));
        assertThat(options.useConsumerGroups, is(false));
        assertThat(options.minPartition, is(2));
        assertThat(options.maxPartition, is(4));
        assertThat(options.pointTopic, is("omgPOINTS"));
    }
}