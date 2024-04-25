package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static org.mitre.openaria.airborne.DataCleaning.requireProximity;
import static org.mitre.openaria.airborne.DataCleaning.requireSeparationFilter;
import static org.mitre.openaria.smoothing.TrackSmoothing.simpleSmoothing;
import static org.mitre.openaria.trackpairing.TrackPairFilters.tracksMustOverlapInTime;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.mitre.caasd.commons.CachingCleaner;
import org.mitre.caasd.commons.CompositeCleaner;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.TrackPairCleaner;
import org.mitre.openaria.smoothing.TrimSlowMovingPointsWithSimilarAltitudes;
import org.mitre.openaria.trackpairing.IsFormationFlight;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * This is a replacement class for AirborneProperties
 */
public class AirborneYamlDefinition {

    /*
     * This class is implemented to optimize for "yaml usage".  Ideally, the fields in this class
     * would all be final.  Unfortunately, the jackson yaml parsing library makes that difficult.
     *
     * The issues are that:  (1) String fields cannot be final, if they are the value of the string
     * set when calling the no-arg constructor cannot be changed by a yaml configuration input
     * (unless the no-arg constructor set the String to null).  (2) final primitive fields (e.g.,
     * maxReportableScore) cannot be changed by the Jackson library.  However, if you used a boxed
     * type (i.e. Integer) in place of a primitive type (i.e. int) Jackson COULD alter the final
     * field.
     *
     * Essentially, jackson has unpredictable behavior when dealing with final fields.  The
     * compromise is to make all fields non-final but ensure there is no API path to alter the
     * fields (thus making them quasi-final)..
     */

    private String hostId = "airborne-compute-1";

    private double maxReportableScore = 20.0;

    private boolean filterByAirspace = true;

    private double requiredDiverganceDistInNM = 0.5;

    private double onGroundSpeedInKnots = 80.0;

    private long requiredTimeOverlapInMs = 7500L;

    private String formationFilters = "0.5,60,false";  //"0.5,60,false|0.75,120,false|1.5,300,false";

    private double requiredProximityInNM = 7.5;

    private int sizeOfTrackSmoothingCache = 500;

    private int trackSmoothingExpirationSec = 120;

    private boolean logDuplicateTracks = false;

    private boolean applySmoothing = true;

    private boolean requireDataTag = true;

    private boolean publishAirborneDynamics = true;

    private boolean publishTrackData = false;

    private boolean verbose = false;

    private String logFileDirectory = "logs";

    private double airborneDynamicsRadiusNm = 15.0;

    //This field is created on demand
    private DataCleaner<Track> sharedTrackCleaner;


    /**
     * @return An AirborneYamlConfig which contains all default values for the Airborne ARIA
     *     algorithm. This configuration is returned as a mutable Properties object to aid creating
     *     custom configurations. For example, it will often be easier to use these default
     *     asProperties and change a single cherry-picked property rather than manually setting 12
     *     or more asProperties when only 1 or two of them will change.
     */
    private AirborneYamlDefinition() {
        //Initialize all fields to their default values
    }

    public static AirborneYamlDefinition defaultConfig() {
        return new AirborneYamlDefinition();
    }

    /** Parse a YAML file that specifies a single AirborneYamlConfig. */
    public static AirborneYamlDefinition fromYaml(File yamlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        return mapper.readValue(yamlFile, AirborneYamlDefinition.class);
    }

    public String hostId() {
        return this.hostId;
    }

    /**
     * Two tracks MUST diverge by at least this distance before an Airborne event can be created.
     * This rule prevents false positives when an aircraft in the sky changes its beacon
     * information. The change cause track data to temporarily get mirrored in two different tracks
     * (one with the new beacon information and one with the old beacon information).
     *
     * <p>This parameter is required.
     *
     * @return The distance of this parameter in nautical miles
     */
    public double requiredDiverganceDistInNM() {
        return this.requiredDiverganceDistInNM;
    }

    /**
     * Airborne events should not occur if either aircraft is on the ground. Consequently,
     * individual tracks have their "ground points" removed so that they cannot create events while
     * they are on the ground.
     *
     * <p>This parameter is required.
     *
     * @return The speed, in knots, below which we assume aircraft are on the ground.
     */
    public double onGroundSpeedInKnots() {
        return this.onGroundSpeedInKnots;
    }

    public List<IsFormationFlight.FormationFilterDefinition> formationFilterDefs() {
        String definition = this.formationFilters;
        return IsFormationFlight.parseMultipleFilterDefs(definition);
    }

    /**
     * This parameter is optional, if a value isn't supplied in the constructor the default (false)
     * will be used.
     *
     * @return If track pairs that are filter out because they never achieved a minimum separation
     *     are written to a dedicated log directory
     */
    public boolean logDuplicateTracks() {
        return this.logDuplicateTracks;
    }

    /**
     * @return The amount of time two tracks must overlap in time to be eligible for event
     *     detection.
     */
    public Duration requiredTimeOverlap() {
        return Duration.ofMillis(this.requiredTimeOverlapInMs);
    }

    /** @return The maximum event score that warrants archiving the corresponding event. */
    public double maxReportableScore() {
        return maxReportableScore;
    }

    /**
     * @return The maximum number of Track, SmoothedTrack pairs kept in the cache. If this value is
     *     not set 500 is returned.
     */
    public int trackSmoothingCacheSize() {
        return this.sizeOfTrackSmoothingCache;
    }

    /**
     * @return The Duration after which Tracks in the smoothing cache expire. If this value is not
     *     set a Duration of 2 minutes is returned.
     */
    public Duration trackSmoothingCacheExpiration() {
        return Duration.ofSeconds(this.trackSmoothingExpirationSec);
    }

    public Distance requiredProximity() {
        return Distance.ofNauticalMiles(this.requiredProximityInNM);
    }

    /**
     * When this is true the stream of Airborne Events WILL NOT contain events that occur outside of
     * a NOP Facility's airspace.
     *
     * @return The value defined at construction.
     */
    public boolean filterByAirspace() {
        return this.filterByAirspace;
    }

    /**
     * This property, which is true by default, should be set to false if the data being ingest by
     * ARIA is already smoothed.
     */
    public boolean applySmoothing() {
        return this.applySmoothing;
    }

    public boolean requireAtLeastOneDataTag() {
        return this.requireDataTag;
    }

    /**
     * This property governs how the Airborne algorithm is configure before it is embedded in the
     * TrackPairProcessor provided via the "trackPairProcessor()" method.
     *
     * @return The value of the "verbose" property key.
     */
    public boolean verbose() {
        return this.verbose;
    }

    /**
     * @return A DataCleaner that caches the results obtained when smoothing individual Tracks. The
     *     goal is to ensure that when a particular Track is a part of multiple TrackPairs then that
     *     Track does not need to be cleaned multiple times. For example, say Track_A is one of the
     *     Tracks in 11 different TrackPairs. We do not want to smooth Track_A 11 different times.
     *     We'd prefer to smooth Track_A exactly once and reuse the result. It is also important to
     *     note that this CachingCleaner should be shared across multiple facilities to ensure that
     *     (A) the amount of Memory dedicated to this CachingCleaner is well understood and (B) that
     *     memory is not permanently dedicated to caching data for Facilities that are no longer
     *     actively processing data.
     */
    synchronized DataCleaner<Track> singleTrackCleaner() {

        if (this.sharedTrackCleaner == null) {
            this.sharedTrackCleaner = new CachingCleaner(
                simpleSmoothing(), //the result from this smoothing operation frequently gets recomputed
                trackSmoothingCacheSize(),
                trackSmoothingCacheExpiration()
            );
        }
        return this.sharedTrackCleaner;
    }

    /**
     * @return A DataCleaner that removes events that are deemed false positives by the staff at
     *     AJI. For example, this DataCleaner will repress TrackPairs that are "flying in formation"
     *     and TrackPairs that are probably both tracking the same aircraft.
     */
    public DataCleaner<TrackPair> pairCleaner() {

        return applySmoothing()
            ? makeTrackPairCleaner()
            : noOpTrackPairCleaner();
    }

    private DataCleaner<TrackPair> makeTrackPairCleaner() {
        final int MIN_NUM_TRACK_POINTS = 9;

        List<DataCleaner<TrackPair>> cleaningSteps = newArrayList();

        //only consider Track pairs in which at least one aircraft had a ACID (was using ATC)
        if (requireAtLeastOneDataTag()) {
            cleaningSteps.add(new RequireAtLeastOneAircraftId());
        }
        //perform general purpose data clean-up
        cleaningSteps.add(TrackPairCleaner.from(singleTrackCleaner()));

        //remove ground (i.e. low speed point) data from tracks
        cleaningSteps.add(TrackPairCleaner.from(
            new TrimSlowMovingPointsWithSimilarAltitudes(
                Speed.of(onGroundSpeedInKnots(), Speed.Unit.KNOTS),
                Distance.ofFeet(150), //Make me a property -- maybe...if someone needs it
                MIN_NUM_TRACK_POINTS
            ))
        );

        //confirm tracks overlap the required amount AFTER removing ground data
        cleaningSteps.add(tracksMustOverlapInTime(requiredTimeOverlap()));

        //ignore tracks that do not come within 7.5 NM and 5,000ft
        Distance lateralReq = Distance.ofNauticalMiles(7.5);
        Distance verticalReq = Distance.ofFeet(5000);
        cleaningSteps.add(requireProximity(lateralReq, verticalReq));

        //ignore track pairs that are ALWAYS within this distance of each other
        //this filter is intended to remove track pairs that follow the same aircraft
        cleaningSteps.add(requireSeparationFilter(this.logDuplicateTracks, this.requiredDiverganceDistInNM));

        //NOTE -- THE FORMATION FLIGHT FILTER IS NOW APPLIED AFTER THE MIN-SCORE EVENT IS FOUND

        return new CompositeCleaner(cleaningSteps);
    }

    /** @return a DataCleaner that does nothing to the input TrackPair. */
    private DataCleaner<TrackPair> noOpTrackPairCleaner() {
        return TrackPairCleaner.from(
            (Track track) -> Optional.of(track)
        );
    }

    /**
     * @return {@code true} if {@link AirborneEvent#airborneDynamics()} should be non-null, {@code
     *     false} otherwise
     */
    public boolean publishDynamics() {
        return this.publishAirborneDynamics;
    }

    public Distance dynamicsInclusionRadius() {
        return Distance.ofNauticalMiles(this.airborneDynamicsRadiusNm);
    }

    /**
     * @return Governs if the Airborne ARIA algorithm will include raw track data in the
     *     AirborneEvents it generates as it processes data.
     */
    public boolean publishTrackData() {
        return this.publishTrackData;
    }

    /**
     * This property specifies the name of the the directory into which logs are written
     *
     * @return The value of the "logFileDirectory" property key.
     */
    public String logFileDirectory() {
        return this.logFileDirectory;
    }
}
