package org.mitre.openaria.core.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;


public class PluginFactoryTest {

    public interface InterfacePluginBuildersMustImplement {
        public int injectedStrategyBehavior(Integer handleMe);
    }

    public static class SimplePlugin implements InterfacePluginBuildersMustImplement {
        @Override
        public int injectedStrategyBehavior(Integer handleMe) {
            return handleMe;
        }
    }

    public static class ConfigurablePlugin implements InterfacePluginBuildersMustImplement, YamlConfigured {

        Integer otherNumber;

        @Override
        public int injectedStrategyBehavior(Integer handleMe) {
            return otherNumber + handleMe;
        }

        @Override
        public void configure(Map<String, ?> configs) {
            this.otherNumber = (Integer) configs.get("otherNumber"); //here we rely on Yaml parser to deliver a number and not a String
        }
    }

    @Test
    public void canBuildSimplePluginFromYaml() throws IOException {

        //load the yaml file that "wants to build" a SimplePlugin
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("injectedSimplePlugin.yaml").getFile());

        //Setup yaml parser
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        //Use the yaml parser to get an instance of PluginFactory
        PluginFactory ic = mapper.readValue(yamlFile, PluginFactory.class);

        //Validate the data the PluginFactory has...
        assertThat(ic.pluginClass(), is("org.mitre.openaria.core.config.PluginFactoryTest$SimplePlugin"));
        assertThat(ic.configOptions().size(), is(2));
        assertThat(ic.configOptions().get("keyA"), is("hello"));
        assertThat(ic.configOptions().get("keyB"), is(5));

        //Use the PluginFactory to actually build the Plugin we want..
        SimplePlugin plugin = (SimplePlugin) ic.createConfiguredInstance(InterfacePluginBuildersMustImplement.class);

        assertThat(plugin, instanceOf(SimplePlugin.class));
        assertThat(plugin, instanceOf(InterfacePluginBuildersMustImplement.class));
    }

    @Test
    public void canBuildConfigurablePluginFromYaml() throws IOException {

        //load the yaml file that "wants to build" a ConfigurablePlugin
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("injectedConfigurablePlugin.yaml").getFile());

        //Setup yaml parser
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        //Use the yaml parser to get an instance of PluginFactory
        PluginFactory ic = mapper.readValue(yamlFile, PluginFactory.class);

        //Validate the data the PluginFactory has...
        assertThat(ic.pluginClass(), is("org.mitre.openaria.core.config.PluginFactoryTest$ConfigurablePlugin"));
        assertThat(ic.configOptions().size(), is(3));
        assertThat(ic.configOptions().get("keyA"), is("hello"));
        assertThat(ic.configOptions().get("keyB"), is(5));
        assertThat(ic.configOptions().get("otherNumber"), is(2));


        //Use the PluginFactory to actually build the Plugin we want..
        ConfigurablePlugin plugin = (ConfigurablePlugin) ic.createConfiguredInstance(InterfacePluginBuildersMustImplement.class);

        assertThat(plugin.injectedStrategyBehavior(5), is(7));  //should yield 5 + 2

        assertThat(plugin, instanceOf(ConfigurablePlugin.class));
        assertThat(plugin, instanceOf(InterfacePluginBuildersMustImplement.class));
    }


}