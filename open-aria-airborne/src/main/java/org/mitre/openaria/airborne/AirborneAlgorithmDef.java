package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.util.DemotedException.demote;
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
import org.mitre.openaria.core.formats.Format;
import org.mitre.openaria.core.formats.Formats;
import org.mitre.openaria.smoothing.TrimSlowMovingPointsWithSimilarAltitudes;
import org.mitre.openaria.trackpairing.IsFormationFlight;
import org.mitre.openaria.trackpairing.IsFormationFlight.FormationFilterDefinition;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class AirborneAlgorithmDef {

    private DataCleaner<Track> sharedTrackCleaner;

    // dataFormat must be "nop" or "csv" until additional formats are supported.
    // See also: org.mitre.openaria.core.formats;  (in open-aria-core module)
    private final String dataFormat;
    private final String hostId;
    private final double maxReportableScore;
    private final boolean filterByAirspace;
    private final double requiredDiverganceDistInNM;
    private final double onGroundSpeedInKnots;
    private final long requiredTimeOverlapInMs;
    private final String formationFilters;
    private final double requiredProximityInNM;
    private final int sizeOfTrackSmoothingCache;
    private final int trackSmoothingExpirationSec;
    private final boolean logDuplicateTracks;
    private final boolean applySmoothing;
    private final boolean requireDataTag;
    private final boolean publishAirborneDynamics;
    private final boolean publishTrackData;
    private final boolean verbose;
    private final String logFileDirectory;
    private final double airborneDynamicsRadiusNm;


    public AirborneAlgorithmDef() {
        this(defaultBuilder());
    }

    private AirborneAlgorithmDef(Builder builder) {
        this.dataFormat = builder.dataFormat;
        this.hostId = builder.hostId;
        this.maxReportableScore = builder.maxReportableScore;
        this.filterByAirspace = builder.filterByAirspace;
        this.requiredDiverganceDistInNM = builder.requiredDiverganceDistInNM;
        this.onGroundSpeedInKnots = builder.onGroundSpeedInKnots;
        this.requiredTimeOverlapInMs = builder.requiredTimeOverlapInMs;
        this.formationFilters = builder.formationFilters;
        this.requiredProximityInNM = builder.requiredProximityInNM;
        this.sizeOfTrackSmoothingCache = builder.sizeOfTrackSmoothingCache;
        this.trackSmoothingExpirationSec = builder.trackSmoothingExpirationSec;
        this.logDuplicateTracks = builder.logDuplicateTracks;
        this.applySmoothing = builder.applySmoothing;
        this.requireDataTag = builder.requireDataTag;
        this.publishAirborneDynamics = builder.publishAirborneDynamics;
        this.publishTrackData = builder.publishTrackData;
        this.verbose = builder.verbose;
        this.logFileDirectory = builder.logFileDirectory;
        this.airborneDynamicsRadiusNm = builder.airborneDynamicsRadiusNm;
    }

    /** @return The AirborneAlgorithmDef represented by the provided YAML file. */
    public static AirborneAlgorithmDef specFromYaml(File yamlFile) {
        return new AirborneAlgorithmDef(parseYaml(yamlFile));
    }

    private static Builder parseYaml(File yamlFile) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        try {
            return mapper.readValue(yamlFile, Builder.class);
        } catch (IOException ioe) {
            throw demote("Failed to parse yaml file", ioe);
        }
    }

    public Format<?> dataFormat() {
        return Formats.getFormat(dataFormat);
    }

    public String hostId() {
        return hostId;
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
        return requiredDiverganceDistInNM;
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
        return onGroundSpeedInKnots;
    }

    public List<FormationFilterDefinition> formationFilterDefs() {
        String definition = formationFilters;
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
        return logDuplicateTracks;
    }

    /**
     * @return The amount of time two tracks must overlap in time to be eligible for event
     *     detection.
     */
    public Duration requiredTimeOverlap() {
        return Duration.ofMillis(requiredTimeOverlapInMs);
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
        return sizeOfTrackSmoothingCache;
    }

    /**
     * @return The Duration after which Tracks in the smoothing cache expire. If this value is not
     *     set a Duration of 2 minutes is returned.
     */
    public Duration trackSmoothingCacheExpiration() {
        return Duration.ofSeconds(trackSmoothingExpirationSec);
    }

    public Distance requiredProximity() {
        return Distance.ofNauticalMiles(requiredProximityInNM);
    }

    /**
     * When this is true the stream of Airborne Events WILL NOT contain events that occur outside of
     * a NOP Facility's airspace.
     *
     * @return The value defined at construction.
     */
    public boolean filterByAirspace() {
        return filterByAirspace;
    }

    /**
     * This property, which is true by default, should be set to false if the data being ingest by
     * ARIA is already smoothed.
     */
    public boolean applySmoothing() {
        return applySmoothing;
    }

    public boolean requireAtLeastOneDataTag() {
        return requireDataTag;
    }

    /**
     * This property governs how the Airborne algorithm is configure before it is embedded in the
     * TrackPairProcessor provided via the "trackPairProcessor()" method.
     *
     * @return The value of the "verbose" property key.
     */
    public boolean verbose() {
        return verbose;
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
        return TrackPairCleaner.from(track -> Optional.of(track));
    }

    /**
     * @return {@code true} if {@link AirborneEvent#airborneDynamics()} should be non-null,
     *     {@code false} otherwise
     */
    public boolean publishDynamics() {
        return publishAirborneDynamics;
    }

    public Distance dynamicsInclusionRadius() {
        return Distance.ofNauticalMiles(airborneDynamicsRadiusNm);
    }

    /**
     * @return Governs if the Airborne ARIA algorithm will include raw track data in the
     *     AirborneEvents it generates as it processes data.
     */
    public boolean publishTrackData() {
        return publishTrackData;
    }

    /**
     * This property specifies the name of the the directory into which logs are written
     *
     * @return The value of the "logFileDirectory" property key.
     */
    public String logFileDirectory() {
        return logFileDirectory;
    }


    public static Builder defaultBuilder() {
        return new Builder();
    }

    /**
     * This class is designed to be instantiated ONLY via parsing a YAML file.
     * <p>
     * In this case (where the configuration is very simple) we could probably skip using a dedicate
     * Builder object. But if the Object we want to build is complicated using YAML to instantiate a
     * Builder is useful. This means the Builder can build the complicated thing and we don't have
     * to alter the "complicated target object" to make it YAML-friendly.
     */
    public static class Builder {

        private String dataFormat;
        private String hostId;
        private double maxReportableScore;
        private boolean filterByAirspace;
        private double requiredDiverganceDistInNM;
        private double onGroundSpeedInKnots;
        private long requiredTimeOverlapInMs;
        private String formationFilters;
        private double requiredProximityInNM;
        private int sizeOfTrackSmoothingCache;
        private int trackSmoothingExpirationSec;
        private boolean logDuplicateTracks;
        private boolean applySmoothing;
        private boolean requireDataTag;
        private boolean publishAirborneDynamics;
        private boolean publishTrackData;
        private boolean verbose;
        private String logFileDirectory;
        private double airborneDynamicsRadiusNm;


        private Builder() {
            //exists to enable automatic YAML parsing with Jackson library
            this.dataFormat = "csv";
            this.hostId = "airborne-compute-1";
            this.maxReportableScore = 20.0;
            this.filterByAirspace = true;
            this.requiredDiverganceDistInNM = 0.5;
            this.onGroundSpeedInKnots = 80.0;
            this.requiredTimeOverlapInMs = 7500L;
            this.formationFilters = "0.5,60,false";  //"0.5,60,false|0.75,120,false|1.5,300,false";
            this.requiredProximityInNM = 7.5;
            this.sizeOfTrackSmoothingCache = 500;
            this.trackSmoothingExpirationSec = 120;
            this.logDuplicateTracks = false;
            this.applySmoothing = true;
            this.requireDataTag = true;
            this.publishAirborneDynamics = true;
            this.publishTrackData = false;
            this.verbose = false;
            this.logFileDirectory = "logs";
            this.airborneDynamicsRadiusNm = 15.0;
        }

        public Builder dataFormat(String dataFormat) {
            requireNonNull(dataFormat, "AirborneAlgorithmDef's dataFormat must be set");
            Formats.getFormat(dataFormat); // eagerly pull the Format to fail early
            this.dataFormat = dataFormat;
            return this;
        }

        public Builder hostId(String hostId) {
            requireNonNull(hostId);
            this.hostId = hostId;
            return this;
        }

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
            return new AirborneAlgorithmDef(this);
        }
    }
}