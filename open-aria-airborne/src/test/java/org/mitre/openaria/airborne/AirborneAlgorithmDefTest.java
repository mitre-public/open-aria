
package org.mitre.openaria.airborne;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_DYNAMICS_DISTANCE_NM;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_FILTER_BY_AIRSPACES;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_LOG_DUPLICATE_TRACKS;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_LOG_FILE_DIRECTORY;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_MAX_REPORTABLE_SCORE;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_ON_GROUND_SPEED_IN_KNOTS;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_PUBLISH_AIRBORNE_DYNAMICS;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_PUBLISH_TRACK_DATA;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_REQUIRED_PROXIMITY;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_REQ_DIVERANCE_IN_NM;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_REQ_TIME_OVERLAP_IN_MS;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_TRACK_CACHE_EXPIRATION_SEC;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_TRACK_CACHE_SIZE;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.DEFAULT_VERBOSE;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.FILTER_BY_AIRSPACES;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.FORMATION_FILTERS;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.MAX_REPORTABLE_SCORE;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.ON_GROUND_SPEED_IN_KNOTS;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.REQUIRED_TIME_OVERLAP_IN_MS;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.REQ_DIVERGANCE_IN_NM;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.TRACK_SMOOTHING_CACHE_EXPIRATION_SEC;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.TRACK_SMOOTHING_CACHE_SIZE;
import static org.mitre.openaria.airborne.AirborneAlgorithmDef.defaultBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.util.PropertyUtils.MissingPropertyException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class AirborneAlgorithmDefTest {

    public static Properties completePropertySet() {
        Properties props = new Properties();
        props.setProperty("host.id", "useMeToIdentifyTheHostComputer");
        props.setProperty("requiredDiverganceDistInNM", "1.23");
        props.setProperty("onGroundSpeedInKnots", "80.0");
        props.setProperty("logNonDivergingFlights", "false");
        props.setProperty("maxReportableScore", "10");

        props.setProperty(FORMATION_FILTERS, "0.5,60,false");

        props.setProperty(TRACK_SMOOTHING_CACHE_SIZE, "12345");
        props.setProperty(TRACK_SMOOTHING_CACHE_EXPIRATION_SEC, "54321");
        props.setProperty(REQUIRED_TIME_OVERLAP_IN_MS, "7500");
        props.setProperty(FILTER_BY_AIRSPACES, "false");

        return props;
    }

    @Test
    public void testConstructor() {

        AirborneAlgorithmDef props = new AirborneAlgorithmDef(completePropertySet());

        double TOLERANCE = 0.0001;

        assertEquals(props.hostId(), "useMeToIdentifyTheHostComputer");
        assertEquals(props.requiredDiverganceDistInNM(), 1.23, TOLERANCE);
        assertEquals(props.onGroundSpeedInKnots(), 80.0, TOLERANCE);
        assertEquals(props.logDuplicateTracks(), false);
        assertEquals(props.maxReportableScore(), 10.0, TOLERANCE);

        assertEquals(props.trackSmoothingCacheSize(), 12345);
        assertEquals(props.trackSmoothingCacheExpiration(), Duration.ofSeconds(54321));

        assertEquals(props.requiredTimeOverlap(), Duration.ofMillis(7_500));
    }

    /* This method will fail if it doesn't throw an exception 1st. */
    public void confirmPropertyIsRequired(String propertyName) {
        Properties props = completePropertySet();
        props.remove(propertyName);

        AirborneAlgorithmDef airborneProps = new AirborneAlgorithmDef(props);
        fail("FAILED because the required property " + propertyName + " was not set");
    }

    @Test
    public void testMissingDivergance() {

        assertThrows(
            MissingPropertyException.class,
            () -> confirmPropertyIsRequired(REQ_DIVERGANCE_IN_NM)
        );
    }

    @Test
    public void testMissingGroundSpeed() {

        assertThrows(
            MissingPropertyException.class,
            () -> confirmPropertyIsRequired(ON_GROUND_SPEED_IN_KNOTS)
        );
    }

    @Test
    public void testMissingMaxScore() {

        assertThrows(
            MissingPropertyException.class,
            () -> confirmPropertyIsRequired(MAX_REPORTABLE_SCORE)
        );
    }

    @Test
    public void testMissingRequiredTimeOverlap() {
        assertThrows(
            MissingPropertyException.class,
            () -> confirmPropertyIsRequired(REQUIRED_TIME_OVERLAP_IN_MS)
        );
    }

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

        assertThat(props.maxReportableScore(), is(parseDouble(DEFAULT_MAX_REPORTABLE_SCORE)));
        assertThat(props.filterByAirspace(), is(parseBoolean(DEFAULT_FILTER_BY_AIRSPACES)));
        assertThat(props.publishDynamics(), is(parseBoolean(DEFAULT_PUBLISH_AIRBORNE_DYNAMICS)));
        assertThat(props.publishTrackData(), is(parseBoolean(DEFAULT_PUBLISH_TRACK_DATA)));
        assertThat(props.requiredDiverganceDistInNM(), is(parseDouble(DEFAULT_REQ_DIVERANCE_IN_NM)));
        assertThat(props.onGroundSpeedInKnots(), is(parseDouble(DEFAULT_ON_GROUND_SPEED_IN_KNOTS)));
        assertThat(props.requiredTimeOverlap(), is(Duration.ofMillis(parseLong(DEFAULT_REQ_TIME_OVERLAP_IN_MS))));

        //confirm the formation flight filter is properly setup
        assertThat(props.formationFilterDefs(), hasSize(1));
        assertThat(props.formationFilterDefs().get(0).logRemovedFilter, is(false));
        assertThat(props.formationFilterDefs().get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.5)));
        assertThat(props.formationFilterDefs().get(0).timeRequirement, is(Duration.ofSeconds(60)));

        //optional properties
        assertThat(props.requiredProximity(), is(Distance.ofNauticalMiles(parseDouble(DEFAULT_REQUIRED_PROXIMITY))));
        assertThat(props.trackSmoothingCacheSize(), is(parseInt(DEFAULT_TRACK_CACHE_SIZE)));
        assertThat(props.trackSmoothingCacheExpiration(), is(Duration.ofSeconds(parseLong(DEFAULT_TRACK_CACHE_EXPIRATION_SEC))));
        assertThat(props.logDuplicateTracks(), is(parseBoolean(DEFAULT_LOG_DUPLICATE_TRACKS)));
        assertThat(props.verbose(), is(parseBoolean(DEFAULT_VERBOSE)));
        assertThat(props.logFileDirectory(), is(DEFAULT_LOG_FILE_DIRECTORY));
        assertThat(props.dynamicsInclusionRadius(), is(Distance.ofNauticalMiles(parseDouble(DEFAULT_DYNAMICS_DISTANCE_NM))));
    }


    @Test
    public void canBuildFromYaml() throws IOException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File yamlFile = new File(classLoader.getResource("airborneConfig.yaml").getFile());

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        //parse out a Builder....then execute the build method
        AirborneAlgorithmDef def = mapper.readValue(yamlFile, AirborneAlgorithmDef.Builder.class).build();

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
