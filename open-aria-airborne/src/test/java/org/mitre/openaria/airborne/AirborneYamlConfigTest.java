package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Distance;

public class AirborneYamlConfigTest {

    @Test
    public void getDefaultValuesFromNoArgConstructor() {
        /* This constructor supplies all default values. */
        AirborneYamlDefinition config = AirborneYamlDefinition.defaultConfig();

        //Validate all default values
        assertThat(config.hostId(), is("airborne-compute-1"));
        assertThat(config.maxReportableScore(), is(20.0));
        assertThat(config.filterByAirspace(), is(true));
        assertThat(config.requiredDiverganceDistInNM(), is(0.5));
        assertThat(config.onGroundSpeedInKnots(), is(80.0));
        assertThat(config.requiredTimeOverlap(), is(Duration.ofMillis(7_500)));

        assertThat(config.formationFilterDefs(), hasSize(1));
        assertThat(config.formationFilterDefs().get(0).timeRequirement, is(Duration.ofSeconds(60)));
        assertThat(config.formationFilterDefs().get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.50)));
        assertThat(config.formationFilterDefs().get(0).logRemovedFilter, is(false));

        assertThat(config.requiredProximity(), is(Distance.ofNauticalMiles(7.5)));
        assertThat(config.trackSmoothingCacheSize(), is(500));
        assertThat(config.trackSmoothingCacheExpiration(), is(Duration.ofSeconds(120)));
        assertThat(config.logDuplicateTracks(), is(false));
        assertThat(config.applySmoothing(), is(true));
        assertThat(config.requireAtLeastOneDataTag(), is(true));
        assertThat(config.publishDynamics(), is(true));
        assertThat(config.publishTrackData(), is(false));
        assertThat(config.verbose(), is(false));
        assertThat(config.logFileDirectory(), is("logs"));
        assertThat(config.dynamicsInclusionRadius(), is(Distance.ofNauticalMiles(15.0)));
    }


    @Test
    public void canBuildFromYaml() throws IOException {

        //load the yaml file that "wants to build" a SimplePlugin
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("airborneConfig.yaml").getFile());

        /* This Factory method reads the yaml file (which overrides values from the default constructor) */
        AirborneYamlDefinition config = AirborneYamlDefinition.fromYaml(yamlFile);

        //Validate the values in the Yaml file are correctly reflected
        // (i.e. the default values were overridden)
        assertThat(config.hostId(), is("airborne-compute-2"));
        assertThat(config.maxReportableScore(), is(21.0));
        assertThat(config.filterByAirspace(), is(false));
        assertThat(config.requiredDiverganceDistInNM(), is(1.5));
        assertThat(config.onGroundSpeedInKnots(), is(81.0));
        assertThat(config.requiredTimeOverlap(), is(Duration.ofMillis(7_600)));

        assertThat(config.formationFilterDefs(), hasSize(1));
        assertThat(config.formationFilterDefs().get(0).timeRequirement, is(Duration.ofSeconds(61)));
        assertThat(config.formationFilterDefs().get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.60)));
        assertThat(config.formationFilterDefs().get(0).logRemovedFilter, is(true));

        assertThat(config.requiredProximity(), is(Distance.ofNauticalMiles(8.5)));
        assertThat(config.trackSmoothingCacheSize(), is(5000));
        assertThat(config.trackSmoothingCacheExpiration(), is(Duration.ofSeconds(1200)));
        assertThat(config.logDuplicateTracks(), is(true));
        assertThat(config.applySmoothing(), is(false));
        assertThat(config.requireAtLeastOneDataTag(), is(false));
        assertThat(config.publishDynamics(), is(false));
        assertThat(config.publishTrackData(), is(true));
        assertThat(config.verbose(), is(true));
        assertThat(config.logFileDirectory(), is("notLogs"));
        assertThat(config.dynamicsInclusionRadius(), is(Distance.ofNauticalMiles(25.0)));
    }
}