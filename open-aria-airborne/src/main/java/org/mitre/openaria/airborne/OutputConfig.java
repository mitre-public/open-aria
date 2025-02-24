package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.mitre.caasd.commons.out.OutputSink;
import org.mitre.openaria.core.config.PluginFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * The OutputConfig configures how Airborne ARIA events are published/emitted from the system.
 *
 * <p>OutputConfig are built by first creating an OutputConfig.Builder by parsing YAML.
 *
 * <p>The chief design goal of OutputConfig is to allow "injecting unknown OutputSink plugins"
 * without recompiling this class.
 */
public class OutputConfig {

    private final List<OutputSink<AirborneEvent>> outputSinks;

    private final OutputSink<AirborneEvent> combinedSink;

    private OutputConfig(Builder builder) {
        this.outputSinks = builder.sinks();
        this.combinedSink = OutputSink.combine(outputSinks);
    }

    public static OutputConfig fromYaml(File yamlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        //Parse out a Builder...then build it.
        return mapper.readValue(yamlFile, OutputConfig.Builder.class).build();
    }

    /**
     * This method always return the same OutputSink instance. The prevents accidentally creating
     * multiple connections to external databases, multiple FileWriters that send output to the same
     * target file, etc.
     *
     * @return Where AirborneEvents will be sent/archived/reported.
     */
    public OutputSink<AirborneEvent> outputSink() {
        return combinedSink;
    }

    /** @return The list of all OutputSinks that were loaded from the Yaml. */
    public List<OutputSink<AirborneEvent>> sinks() {
        return this.outputSinks;
    }

    /**
     * This Builder is designed to be instantiated via a YAML file.  If necessary the typical
     * builder pattern methods can be used (mostly supplied for unit testing)
     */
    public static class Builder {

        private PluginFactory[] outputSinkSuppliers;  //Expects Supplier<OutputSink>..

        private Builder() {
            //exists to enable automatic YAML parsing with Jackson library
            this.outputSinkSuppliers = null;
        }

        private List<OutputSink<AirborneEvent>> sinks() {

            List<OutputSink<AirborneEvent>> listOfSink = newArrayList();

            for (PluginFactory factory : outputSinkSuppliers) {
                Supplier<OutputSink<AirborneEvent>> supplier = factory.createConfiguredInstance(Supplier.class);
                OutputSink<AirborneEvent> sink = supplier.get();
                listOfSink.add(sink);
            }
            return listOfSink;
        }


        public OutputConfig build() {
            requireNonNull(outputSinkSuppliers);

            return new OutputConfig(this);
        }
    }
}