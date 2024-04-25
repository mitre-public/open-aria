package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;
import static org.mitre.openaria.airborne.DataCleaning.requireProximity;
import static org.mitre.openaria.airborne.DataCleaning.requireSeparationFilter;
import static org.mitre.openaria.smoothing.TrackSmoothing.simpleSmoothing;
import static org.mitre.openaria.trackpairing.TrackPairFilters.tracksMustOverlapInTime;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.mitre.caasd.commons.CachingCleaner;
import org.mitre.caasd.commons.CompositeCleaner;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.util.ImmutableConfig;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.TrackPairCleaner;
import org.mitre.openaria.smoothing.TrimSlowMovingPointsWithSimilarAltitudes;
import org.mitre.openaria.trackpairing.IsFormationFlight;
import org.mitre.openaria.trackpairing.IsFormationFlight.FormationFilterDefinition;

public class AirborneAlgorithmDef {

    //REQUIRED PROPERTIES -- must be supplied when using the Constructor that accepts a Properties object.
    //The "default" value for these asProperties are used when calling the "no-arg" constructor

    public static final String HOST_ID = "host.id";
    public static final String DEFAULT_HOST_ID = "airborne-compute-1";

    public static final String MAX_REPORTABLE_SCORE = "maxReportableScore";
    public static final String DEFAULT_MAX_REPORTABLE_SCORE = "20";

    public static final String FILTER_BY_AIRSPACES = "filterByAirspace";
    public static final String DEFAULT_FILTER_BY_AIRSPACES = "true";

    public static final String REQ_DIVERGANCE_IN_NM = "requiredDiverganceDistInNM";
    public static final String DEFAULT_REQ_DIVERANCE_IN_NM = "0.5";

    public static final String ON_GROUND_SPEED_IN_KNOTS = "onGroundSpeedInKnots";
    public static final String DEFAULT_ON_GROUND_SPEED_IN_KNOTS = "80.0";

    public static final String REQUIRED_TIME_OVERLAP_IN_MS = "requiredTimeOverlapInMilliSec";
    public static final String DEFAULT_REQ_TIME_OVERLAP_IN_MS = "7500";

    public static final String FORMATION_FILTERS = "formationFilters";
    public static final String DEFAULT_FORMATION_FILTERS = "0.5,60,false";
    //public static final String DEFAULT_FORMATION_FILTERS = "0.5,60,false|0.75,120,false|1.5,300,false";

    //OPTIONAL PROPERTIES -- These asProperties do not need to be specified when using the Constructor that accepts a Properties object.
    public static final String REQUIRE_PROXIMITY = "requiredProximityInNM";
    public static final String DEFAULT_REQUIRED_PROXIMITY = "7.5";

    public static final String TRACK_SMOOTHING_CACHE_SIZE = "sizeOfTrackSmoothingCache";
    public static final String DEFAULT_TRACK_CACHE_SIZE = "500";

    public static final String TRACK_SMOOTHING_CACHE_EXPIRATION_SEC = "trackSmoothingExpirationSec";
    public static final String DEFAULT_TRACK_CACHE_EXPIRATION_SEC = "120";

    public static final String LOG_DUPLICATE_TRACKS = "logDuplicateTracks";
    public static final String DEFAULT_LOG_DUPLICATE_TRACKS = "false";

    public static final String VERBOSE_KEY = "verbose";
    public static final String DEFAULT_VERBOSE = "false";

    //turning off smoothing is necessary for integration with TDP -- where Tracks are already smoothed
    public static final String APPLY_SMOOTHING = "applySmoothing";
    public static final String DEFAULT_APPLY_SMOOTHING = "true";

    //this is a requirement of the ARIA program -- but it can be turned off
    public static final String REQUIRE_A_DATA_TAG = "requireDataTag";
    public static final String DEFAULT_REQUIRE_A_DATA_TAG = "true";

    public static final String PUBLISH_AIRBORNE_DYNAMICS = "publishAirborneDynamics";
    public static final String DEFAULT_PUBLISH_AIRBORNE_DYNAMICS = "false";

    public static final String DYNAMICS_INCLUSION_DISTANCE_NM = "airborne.dynamics.radius.nm";
    public static final String DEFAULT_DYNAMICS_DISTANCE_NM = "15.0";

    public static final String PUBLISH_TRACK_DATA = "publishTrackData";
    public static final String DEFAULT_PUBLISH_TRACK_DATA = "false";

    public static final String LOG_FILE_DIRECTORY = "logFileDirectory";
    public static final String DEFAULT_LOG_FILE_DIRECTORY = "logs";

    public static final List<String> REQUIRED_PROPS = newArrayList(
        HOST_ID,
        MAX_REPORTABLE_SCORE,
        FILTER_BY_AIRSPACES,
        REQ_DIVERGANCE_IN_NM,
        ON_GROUND_SPEED_IN_KNOTS,
        REQUIRED_TIME_OVERLAP_IN_MS,
        FORMATION_FILTERS
    );

    private DataCleaner<Track> sharedTrackCleaner;

    private final ImmutableConfig config;

    public AirborneAlgorithmDef() {
        this(defaultProperties());
    }

    public AirborneAlgorithmDef(Properties props) {
        this.config = new ImmutableConfig(props, REQUIRED_PROPS);
    }

    public AirborneAlgorithmDef(File source) {
        this(loadProperties(source));
    }

    public String hostId() {
        return config.getString(HOST_ID);
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
        return config.getDouble(REQ_DIVERGANCE_IN_NM);
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
        return config.getDouble(ON_GROUND_SPEED_IN_KNOTS);
    }

    public List<FormationFilterDefinition> formationFilterDefs() {
        String definition = config.getString(FORMATION_FILTERS);
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
        return config.getOptionalBoolean(LOG_DUPLICATE_TRACKS)
            .orElse(parseBoolean(DEFAULT_LOG_DUPLICATE_TRACKS));
    }

    /**
     * @return The amount of time two tracks must overlap in time to be eligible for event
     *     detection.
     */
    public Duration requiredTimeOverlap() {
        return Duration.ofMillis(config.getInt(REQUIRED_TIME_OVERLAP_IN_MS));
    }

    /** @return The maximum event score that warrants archiving the corresponding event. */
    public double maxReportableScore() {
        return config.getDouble(MAX_REPORTABLE_SCORE);
    }

    /**
     * @return The maximum number of Track, SmoothedTrack pairs kept in the cache. If this value is
     *     not set 500 is returned.
     */
    public int trackSmoothingCacheSize() {
        return config.getOptionalInt(TRACK_SMOOTHING_CACHE_SIZE)
            .orElse(parseInt(DEFAULT_TRACK_CACHE_SIZE));
    }

    /**
     * @return The Duration after which Tracks in the smoothing cache expire. If this value is not
     *     set a Duration of 2 minutes is returned.
     */
    public Duration trackSmoothingCacheExpiration() {
        long numSec = config.getOptionalLong(TRACK_SMOOTHING_CACHE_EXPIRATION_SEC)
            .orElse((parseLong(DEFAULT_TRACK_CACHE_EXPIRATION_SEC)));

        return Duration.ofSeconds(numSec);
    }

    public Distance requiredProximity() {
        double distInNm = config.getOptionalDouble(REQUIRE_PROXIMITY)
            .orElse(parseDouble(DEFAULT_REQUIRED_PROXIMITY));

        return Distance.ofNauticalMiles(distInNm);
    }

    /**
     * When this is true the stream of Airborne Events WILL NOT contain events that occur outside of
     * a NOP Facility's airspace.
     *
     * @return The value defined at construction.
     */
    public boolean filterByAirspace() {
        return config.getBoolean(FILTER_BY_AIRSPACES);
    }

    /**
     * This property, which is true by default, should be set to false if the data being ingest by
     * ARIA is already smoothed.
     */
    public boolean applySmoothing() {
        return config.getOptionalBoolean(APPLY_SMOOTHING)
            .orElse(parseBoolean(DEFAULT_APPLY_SMOOTHING));
    }

    public boolean requireAtLeastOneDataTag() {
        return config.getOptionalBoolean(REQUIRE_A_DATA_TAG)
            .orElse(parseBoolean(DEFAULT_REQUIRE_A_DATA_TAG));

    }

    /**
     * This property governs how the Airborne algorithm is configure before it is embedded in the
     * TrackPairProcessor provided via the "trackPairProcessor()" method.
     *
     * @return The value of the "verbose" property key.
     */
    public boolean verbose() {
        return config.getOptionalBoolean(VERBOSE_KEY)
            .orElse(parseBoolean(DEFAULT_VERBOSE));
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
        cleaningSteps.add(requireSeparationFilter(this.logDuplicateTracks(), this.requiredDiverganceDistInNM()));

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
        return config.getOptionalBoolean(PUBLISH_AIRBORNE_DYNAMICS)
            .orElse(parseBoolean(DEFAULT_PUBLISH_AIRBORNE_DYNAMICS));
    }

    public Distance dynamicsInclusionRadius() {
        double nauticalMiles = config.getOptionalDouble(DYNAMICS_INCLUSION_DISTANCE_NM)
            .orElse(parseDouble(DEFAULT_DYNAMICS_DISTANCE_NM));

        return Distance.ofNauticalMiles(nauticalMiles);
    }

    /**
     * @return Governs if the Airborne ARIA algorithm will include raw track data in the
     *     AirborneEvents it generates as it processes data.
     */
    public boolean publishTrackData() {
        return config.getOptionalBoolean(PUBLISH_TRACK_DATA)
            .orElse(parseBoolean(DEFAULT_PUBLISH_TRACK_DATA));
    }

    /**
     * This property specifies the name of the the directory into which logs are written
     *
     * @return The value of the "logFileDirectory" property key.
     */
    public String logFileDirectory() {
        return config.getOptionalString(LOG_FILE_DIRECTORY)
            .orElse(DEFAULT_LOG_FILE_DIRECTORY);
    }

    /**
     * @return a Properties object which contains the default asProperties for the Airborne ARIA
     *     algorithm. This configuration is returned as a mutable Properties object to aid creating
     *     custom configurations. For example, it will often be easier to use these default
     *     asProperties and change a single cherry-picked property rather than manually setting 12
     *     or more asProperties when only 1 or two of them will change.
     */
    private static Properties defaultProperties() {

        Properties props = new Properties();

        //require asProperties
        props.setProperty(HOST_ID, DEFAULT_HOST_ID);
        props.setProperty(MAX_REPORTABLE_SCORE, DEFAULT_MAX_REPORTABLE_SCORE);
        props.setProperty(FILTER_BY_AIRSPACES, DEFAULT_FILTER_BY_AIRSPACES);

        props.setProperty(REQ_DIVERGANCE_IN_NM, DEFAULT_REQ_DIVERANCE_IN_NM);
        props.setProperty(ON_GROUND_SPEED_IN_KNOTS, DEFAULT_ON_GROUND_SPEED_IN_KNOTS);
        props.setProperty(REQUIRED_TIME_OVERLAP_IN_MS, DEFAULT_REQ_TIME_OVERLAP_IN_MS);
        props.setProperty(FORMATION_FILTERS, DEFAULT_FORMATION_FILTERS);

        //optional asProperties
        props.setProperty(REQUIRE_PROXIMITY, DEFAULT_REQUIRED_PROXIMITY);
        props.setProperty(TRACK_SMOOTHING_CACHE_SIZE, DEFAULT_TRACK_CACHE_SIZE);
        props.setProperty(TRACK_SMOOTHING_CACHE_EXPIRATION_SEC, DEFAULT_TRACK_CACHE_EXPIRATION_SEC);
        props.setProperty(LOG_DUPLICATE_TRACKS, DEFAULT_LOG_DUPLICATE_TRACKS);
        props.setProperty(APPLY_SMOOTHING, DEFAULT_APPLY_SMOOTHING);
        props.setProperty(REQUIRE_A_DATA_TAG, DEFAULT_REQUIRE_A_DATA_TAG);
        props.setProperty(PUBLISH_AIRBORNE_DYNAMICS, DEFAULT_PUBLISH_AIRBORNE_DYNAMICS);
        props.setProperty(PUBLISH_TRACK_DATA, DEFAULT_PUBLISH_TRACK_DATA);
        props.setProperty(VERBOSE_KEY, DEFAULT_VERBOSE);
        props.setProperty(LOG_FILE_DIRECTORY, DEFAULT_LOG_FILE_DIRECTORY);

        return props;
    }

    public static Builder defaultBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String hostId = "airborne-compute-1";
        private double maxReportableScore = 20.0;
        private boolean filterByAirspace = true;
        private double requiredDiverganceDistInNM = 0.5;
        private double onGroundSpeedInKnots = 80.0;
        private long requiredTimeOverlapInMs = 7500L;
        private String formationFilters = "0.005,60,false";  //"0.5,60,false|0.75,120,false|1.5,300,false";
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

        public Builder maxReportableScore(double score) {
            this.maxReportableScore = score;
            return this;
        }

        public Builder publishAirborneDynamics(boolean publishAirborneDynamics) {
            this.publishAirborneDynamics = publishAirborneDynamics;
            return this;
        }

        public Builder requireDataTag(boolean requireDataTag) {
            this.requireDataTag = requireDataTag;
            return this;
        }

        public Builder dynamicsInclusionRadiusInNm(double radius) {
            this.airborneDynamicsRadiusNm = radius;
            return this;
        }

        public Builder formationFilterDefs(String def) {
            this.formationFilters = def;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder filterByAirspace(boolean filterByAirspace) {
            this.filterByAirspace = filterByAirspace;
            return this;
        }


        public AirborneAlgorithmDef build() {

            Properties props = new Properties();

            //require asProperties
            props.setProperty(HOST_ID, hostId);
            props.setProperty(MAX_REPORTABLE_SCORE, Double.toString(maxReportableScore));
            props.setProperty(FILTER_BY_AIRSPACES, Boolean.toString(filterByAirspace));

            props.setProperty(REQ_DIVERGANCE_IN_NM, Double.toString(requiredDiverganceDistInNM));
            props.setProperty(ON_GROUND_SPEED_IN_KNOTS, Double.toString(onGroundSpeedInKnots));
            props.setProperty(REQUIRED_TIME_OVERLAP_IN_MS, Long.toString(requiredTimeOverlapInMs));
            props.setProperty(FORMATION_FILTERS, formationFilters);

            //optional asProperties
            props.setProperty(REQUIRE_PROXIMITY, Double.toString(requiredProximityInNM));
            props.setProperty(TRACK_SMOOTHING_CACHE_SIZE, Integer.toString(sizeOfTrackSmoothingCache));
            props.setProperty(TRACK_SMOOTHING_CACHE_EXPIRATION_SEC, Integer.toString(trackSmoothingExpirationSec));
            props.setProperty(LOG_DUPLICATE_TRACKS, Boolean.toString(logDuplicateTracks));
            props.setProperty(APPLY_SMOOTHING, Boolean.toString(applySmoothing));
            props.setProperty(REQUIRE_A_DATA_TAG, Boolean.toString(requireDataTag));
            props.setProperty(PUBLISH_AIRBORNE_DYNAMICS, Boolean.toString(publishAirborneDynamics));
            props.setProperty(PUBLISH_TRACK_DATA, Boolean.toString(publishTrackData));
            props.setProperty(VERBOSE_KEY, Boolean.toString(verbose));
            props.setProperty(LOG_FILE_DIRECTORY, logFileDirectory);
            props.setProperty(DYNAMICS_INCLUSION_DISTANCE_NM, Double.toString(airborneDynamicsRadiusNm));

            return new AirborneAlgorithmDef(props);
        }
    }
}
