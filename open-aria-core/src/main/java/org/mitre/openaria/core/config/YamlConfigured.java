package org.mitre.openaria.core.config;

import java.util.Map;

/**
 * A Mix-in style interface for classes that are instantiated by reflection and need to take
 * configuration parameters
 *
 * <p>Inspired by: org.apache.kafka.common.Configurable
 */
public interface YamlConfigured {

    /**
     * Pass a set of configuration parameters to an object (note: this Map of Key-Value pairs is
     * easy to create by parsing a YAML file).
     */
    void configure(Map<String, ?> configs);
}
