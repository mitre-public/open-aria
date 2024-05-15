
package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.mitre.caasd.commons.Distance;

import org.junit.jupiter.api.Test;

public class AirborneAlgorithmDefTest {


    @Test
    public void defaultCacheSizeIsCorrect() {
        AirborneAlgorithmDef def = defaultBuilder().build();
        assertThat(def.trackSmoothingCacheSize(), is(500));
    }

    @Test
    public void defaultCacheExpirationIsCorrect() {
        AirborneAlgorithmDef def = defaultBuilder().build();
        assertThat(def.trackSmoothingCacheExpiration(), is(Duration.ofSeconds(120)));
    }

    @Test
    public void canConfigureOneFormationFlightFilter() {

        AirborneAlgorithmDef def = defaultBuilder()
            .formationFilterDefs("0.85,92,true")
            .build();

        assertThat(def.formationFilterDefs(), hasSize(1));
        assertThat(def.formationFilterDefs().get(0).timeRequirement, is(Duration.ofSeconds(92)));
        assertThat(def.formationFilterDefs().get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.85)));
        assertThat(def.formationFilterDefs().get(0).logRemovedFilter, is(true));
    }

    @Test
    public void canConfigureMultipleFormationFlightFilters() {

        AirborneAlgorithmDef def = defaultBuilder()
            .formationFilterDefs("0.85,92,true|0.5,60,false")
            .build();

        assertThat(def.formationFilterDefs(), hasSize(2));
        assertThat(def.formationFilterDefs().get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.85)));
        assertThat(def.formationFilterDefs().get(0).timeRequirement, is(Duration.ofSeconds(92)));
        assertThat(def.formationFilterDefs().get(0).logRemovedFilter, is(true));

        assertThat(def.formationFilterDefs().get(1).proximityRequirement, is(Distance.ofNauticalMiles(0.5)));
        assertThat(def.formationFilterDefs().get(1).timeRequirement, is(Duration.ofSeconds(60)));
        assertThat(def.formationFilterDefs().get(1).logRemovedFilter, is(false));
    }

    @Test
    public void canSpecifyDynamicsInclusionRadius() {
        AirborneAlgorithmDef def = defaultBuilder()
            .dynamicsInclusionRadiusInNm(12.3)
            .build();

        assertThat(def.dynamicsInclusionRadius(), is(Distance.ofNauticalMiles(12.3)));
    }

    @Test
    public void noArgConstructorSucceeds() {
        //this should not throw any "MissingPropertyException"
        AirborneAlgorithmDef props = new AirborneAlgorithmDef();
    }

    @Test
    public void noArgConstructorSuppliesCorrectDefaultValues() {

        AirborneAlgorithmDef props = new AirborneAlgorithmDef();

        assertThat(props.maxReportableScore(), is(20.0));
        assertThat(props.filterByAirspace(), is(true));
        assertThat(props.publishDynamics(), is(true));
        assertThat(props.publishTrackData(), is(false));
        assertThat(props.requiredDiverganceDistInNM(), is(0.5));
        assertThat(props.onGroundSpeedInKnots(), is(80.0));
        assertThat(props.requiredTimeOverlap(), is(Duration.ofMillis(7500)));

        //confirm the formation flight filter is properly setup
        assertThat(props.formationFilterDefs(), hasSize(1));
        assertThat(props.formationFilterDefs().get(0).logRemovedFilter, is(false));
        assertThat(props.formationFilterDefs().get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.5)));
        assertThat(props.formationFilterDefs().get(0).timeRequirement, is(Duration.ofSeconds(60)));

        //optional properties
        assertThat(props.requiredProximity(), is(Distance.ofNauticalMiles(7.5)));
        assertThat(props.trackSmoothingCacheSize(), is(500));
        assertThat(props.trackSmoothingCacheExpiration(), is(Duration.ofSeconds(120)));
        assertThat(props.logDuplicateTracks(), is(false));
        assertThat(props.verbose(), is(false));
        assertThat(props.logFileDirectory(), is("logs"));
        assertThat(props.dynamicsInclusionRadius(), is(Distance.ofNauticalMiles(15.0)));
    }


    @Test
    public void canBuildFromYaml() throws IOException {

        File yamlFile = new File("src/test/resources/airborneConfig.yaml");

        AirborneAlgorithmDef def = AirborneAlgorithmDef.specFromYaml(yamlFile);

        //Validate the values in the Yaml file are correctly reflected
        // (i.e. the default values were overridden)
        assertThat(def.hostId(), is("airborne-compute-2"));
        assertThat(def.maxReportableScore(), is(21.0));
        assertThat(def.filterByAirspace(), is(false));
        assertThat(def.requiredDiverganceDistInNM(), is(1.5));
        assertThat(def.onGroundSpeedInKnots(), is(81.0));
        assertThat(def.requiredTimeOverlap(), is(Duration.ofMillis(7_600)));

        assertThat(def.formationFilterDefs(), hasSize(1));
        assertThat(def.formationFilterDefs().get(0).timeRequirement, is(Duration.ofSeconds(61)));
        assertThat(def.formationFilterDefs().get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.60)));
        assertThat(def.formationFilterDefs().get(0).logRemovedFilter, is(true));

        assertThat(def.requiredProximity(), is(Distance.ofNauticalMiles(8.5)));
        assertThat(def.trackSmoothingCacheSize(), is(5000));
        assertThat(def.trackSmoothingCacheExpiration(), is(Duration.ofSeconds(1200)));
        assertThat(def.logDuplicateTracks(), is(true));
        assertThat(def.applySmoothing(), is(false));
        assertThat(def.requireAtLeastOneDataTag(), is(false));
        assertThat(def.publishDynamics(), is(false));
        assertThat(def.publishTrackData(), is(true));
        assertThat(def.verbose(), is(true));
        assertThat(def.logFileDirectory(), is("notLogs"));
        assertThat(def.dynamicsInclusionRadius(), is(Distance.ofNauticalMiles(25.0)));
    }
}
