package org.mitre.openaria.core.config;

import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A PluginFactory helps you "inject a run-time plugin" into an application using a YML
 * configuration file.
 *
 * <p>For example, your application needs a "pluggable" Strategy Object to control a portion of its
 * logic (e.g., InputDeserializer, OutputSerializer, DataPublisher, etc.).  In this case, you are
 * allowing/requiring the user to supply a "plug-in" that integrates with the application and
 * implements some expected interface.  The application's configuration YAML file will need to
 * specify the name of a class that implements the "Strategy Interface" (i.e. the plug-in) and
 * (optionally) supply some additional configuration key-value pairs (see also: YamlConfigured)
 *
 * <p>Once created, an PluginFactory can manufacture fully-configured instances of the
 * "plugin class".
 *
 * <p>In summary, the pattern supported here is: (1) parse an Application-wide YAML, (2) get an
 * PluginFactory for a pluggable component of your application, (3) use the PluginFactory to create
 * configured instances of that component, (4) pass that component into the application.
 */
public class PluginFactory {

    /**
     * The fully qualified name of the class that will be created via reflection.  e.g.,
     * "org.mitre.openaria.ImportantClass" and "org.mitre.openaria.ImportantClass$NestedStaticClass"
     */
    private final String pluginClass;

    /**
     * The configuration options that will be "passed" to new instances of classes that implement
     * YamlConfigured.  These configuration options can be easily specified in a YAML file by
     * creating a list of key-value pairs.
     */
    private final Map<String, ?> configOptions;


    private PluginFactory() {
        //only for YAML reflection...
        pluginClass = null;
        configOptions = null;
    }

    /** Manually create a PluginFactory. */
    public PluginFactory(String pluginClass, Map<String, ?> configOptions) {
        this.pluginClass = pluginClass;
        this.configOptions = configOptions;
    }

    /** Parse a YAML file that specifies a single PluginFactory. */
    public static PluginFactory fromYaml(File yamlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return mapper.readValue(yamlFile, PluginFactory.class);
    }

    /**
     * The fully qualified name of the class that will be created via reflection.  e.g.,
     * "org.mitre.openaria.ImportantClass" and "org.mitre.openaria.ImportantClass$NestedStaticClass"
     */
    public String pluginClass() {
        return this.pluginClass;
    }

    /**
     * Configuration options passed to newly manufactured instances (only applies when the target
     * class implements YamlConfigured).
     */
    public Map<String, ?> configOptions() {
        return this.configOptions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("\npluginClass: " + pluginClass);
        for (Map.Entry<String, ?> entry : configOptions.entrySet()) {
            sb.append("\n  " + entry.getKey() + ": " + entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Create an instance of the "pluginClass" by: (1) using reflection to call the class's no-arg
     * constructor, (2) verifying the "pluginClass" implements the "expectedInterface", (3)
     * calling "YamlConfigured's configure method ONLY IF "pluginClass" implements YamlConfigured.
     *
     * @param expectedInterface An unknown interface "pluginClass" must implement.
     * @param <C> The class we want to generate.
     *
     * @return An instance of "pluginClass" cast to an "expectedInterface"
     */
    public <C> C createConfiguredInstance(Class<C> expectedInterface) {
        //implementation adapted from: org.apache.kafka.common.config.AbstractConfig.getConfiguredInstance(Object klass, Class<T> t, Map<String, Object> configPairs)
        try {
            Class<?> clazz = Class.forName(pluginClass);

            Object o = newInstance(clazz);

            if (!expectedInterface.isInstance(o)) {
                throw new RuntimeException(clazz + " is not an instance of " + expectedInterface.getName());
            }

            //Apply the configuration if appropriate
            if (o instanceof YamlConfigured) {
                ((YamlConfigured) o).configure(configOptions);
            }

            return expectedInterface.cast(o);

        } catch (ClassNotFoundException e) {
            throw demote(e);
        }
    }

    /** Use Reflection to create an instance of Clazz (requires no-arg constructor). */
    private static <A> A newInstance(Class<A> clazz) {
        //implementation adapted from: org.apache.kafka.common.utils.Utils.newInstance(Class<T>)
        requireNonNull(clazz, "class cannot be null");

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw demote("Could not find a public no-argument constructor for " + clazz.getName(), e);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw demote("Could not instantiate class " + clazz.getName(), e);
        }
    }
}
