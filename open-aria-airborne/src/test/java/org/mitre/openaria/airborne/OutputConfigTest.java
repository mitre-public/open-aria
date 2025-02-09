package org.mitre.openaria.airborne;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.mitre.caasd.commons.out.JsonFileSink;
import org.mitre.caasd.commons.out.OutputSink;
import org.mitre.caasd.commons.out.PrintStreamSink;
import org.mitre.openaria.airborne.config.MapSink;
import org.mitre.openaria.kafka.KafkaOutputSink;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

public class OutputConfigTest {

    @Test
    public void canCreateBuilderFromYaml() throws IOException {

        //load the yaml file that "wants to build" a OutputYamlConfig.Builder
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("outputConfig.yaml").getFile());

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        OutputConfig.Builder builder = mapper.readValue(yamlFile, OutputConfig.Builder.class);
    }

    @Test
    public void createCreateFromYaml() throws Exception {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("outputConfig.yaml").getFile());

        OutputConfig config = OutputConfig.fromYaml(yamlFile);

        List<OutputSink<AirborneEvent>> sinks = config.sinks();

        assertThat(sinks, hasSize(3));
        assertThat(sinks.get(0), instanceOf(PrintStreamSink.class));
        assertThat(sinks.get(1), instanceOf(JsonFileSink.class));
        assertThat(sinks.get(2), instanceOf(KafkaOutputSink.class));
    }

    @Test
    public void createCreateFromYaml_withMapSink() throws Exception {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("outputConfig_withMapSink.yaml").getFile());

        OutputConfig config = OutputConfig.fromYaml(yamlFile);

        List<OutputSink<AirborneEvent>> sinks = config.sinks();

        assertThat(sinks, hasSize(2));
        assertThat(sinks.get(0), instanceOf(PrintStreamSink.class));
        assertThat(sinks.get(1), instanceOf(MapSink.class));
    }
}