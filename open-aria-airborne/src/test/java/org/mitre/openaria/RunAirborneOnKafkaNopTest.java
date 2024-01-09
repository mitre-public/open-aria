package org.mitre.openaria;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mitre.openaria.RunAirborneOnKafkaNop.configFromYaml;
import static org.mitre.caasd.commons.util.PropertyUtils.verifyPropertiesAreSet;

import java.io.File;
import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.system.KafkaIngestor;

public class RunAirborneOnKafkaNopTest {

    @Test
    public void canBuildConfigFromYaml() throws Exception {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("fullAirborneConfig.yaml").getFile());

        RunAirborneOnKafkaNop.Config config = configFromYaml(yamlFile);

        config.airFactory(); //fails if the AirborneFactory can't be made
        verifyKafkaProps(config.inputKafkaProps());
        verifyOptions(config.options());
        config.partitionMap(); //fails if the mapping can't be made

        assertThat(config.loggingPeriod(), is(Duration.ofSeconds(15243)));
    }


    private void verifyKafkaProps(Properties inputKafkaProps) {
        verifyPropertiesAreSet(
            inputKafkaProps,
            "bootstrap.servers", "group.id", "key.deserializer", "value.deserializer"
        );
    }

    private void verifyOptions(KafkaIngestor.Options options) {
        assertThat(options.numWorkerThreads, is(40));
        assertThat(options.pointPrefetchLimit, is(12345));
        assertThat(options.milliSecBtwPollAttempts, is(54321));
        assertThat(options.useConsumerGroups, is(true));
        assertThat(options.minPartition, nullValue());
        assertThat(options.maxPartition, nullValue());
        assertThat(options.pointTopic, is("points"));
    }

}