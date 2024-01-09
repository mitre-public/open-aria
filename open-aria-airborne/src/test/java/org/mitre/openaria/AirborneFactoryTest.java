package org.mitre.openaria;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class AirborneFactoryTest {

    @Test
    public void canBuildFromYaml() throws Exception {

        //load the yaml file that "wants to build" a SimplePlugin
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("airborneFactory.yaml").getFile());

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        //parse out a Builder....then execute the build method
        AirborneFactory af = mapper.readValue(yamlFile, AirborneFactory.Builder.class).build();
    }

}