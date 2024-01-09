package org.mitre.openaria.core.config;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


/**
 * Helpful when constructing classes that implement "YamlConfigured" via reflection
 */
public class YamlUtils {

    public static void requireMapKeys(Map<String, ?> configs, Iterable<String> requiredKeys) {
        requiredKeys.forEach(
            key -> checkState(configs.containsKey(key), "Missing configuration key: " + key)
        );
    }

    public static void requireMapKeys(Map<String, ?> configs, String... requiredKeys) {
        Stream.of(requiredKeys).forEach(
            key -> checkState(configs.containsKey(key), "Missing configuration key: " + key)
        );
    }

    /**
     * Parse a YAML file that encodes a class
     *
     * @param yamlFile The .yaml file
     * @param classToCreate The Class the yaml parser will instantiate via reflection
     * @param <T> Same as classToCreate
     *
     * @return An instance of T
     * @throws IOException In the event of a failure when touching the yaml file.
     */
    public static <T> T parseYaml(File yamlFile, Class<T> classToCreate) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        return mapper.readValue(yamlFile, classToCreate);
    }

    /**
     * Parse a YAML contents that encodes a class
     *
     * @param yamlAsString The content of a .yaml file
     * @param classToCreate The Class the yaml parser will instantiate via reflection
     * @param <T> Same as classToCreate
     *
     * @return An instance of T
     * @throws IOException In the event of a failure when touching the yaml file.
     */
    public static <T> T parseYaml(String yamlAsString, Class<T> classToCreate) throws IOException {

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        return mapper.readValue(yamlAsString, classToCreate);
    }
}
