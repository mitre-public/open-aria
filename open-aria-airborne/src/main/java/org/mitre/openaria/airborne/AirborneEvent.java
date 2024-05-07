package org.mitre.openaria.airborne;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Math.abs;
import static org.mitre.caasd.commons.Distance.mean;
import static org.mitre.caasd.commons.Time.asZTimeString;
import static org.mitre.openaria.airborne.SingleAircraftRecord.computeClimbRate;
import static org.mitre.openaria.core.output.HashUtils.hashForJson;
import static org.mitre.openaria.core.output.HashUtils.hashForStringArray;
import static org.mitre.openaria.core.utils.TimeUtils.utcDateAsString;

import java.io.Reader;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.mitre.caasd.commons.Course;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.Speed;
import org.mitre.caasd.commons.Spherical;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.out.JsonWritable;
import org.mitre.openaria.core.AriaEvent;
import org.mitre.openaria.core.IfrVfrStatus;
import org.mitre.openaria.core.PairedIfrVfrStatus;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointPair;
import org.mitre.openaria.core.ScoredInstant;
import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.output.HashUtils;
import org.mitre.openaria.core.utils.ConflictAngle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Top-Level data model for an Airborne ARIA Event.
 *
 * <p>This class is designed so that it converts to "nice" JSON when using serialization tools.
 */
@Immutable
public final class AirborneEvent implements AriaEvent<AirborneEvent> {

    public static final String SCHEMA_VERSION = "3";

    /*
     * The converter is static to ensure the same serialization approach is reused everywhere.
     * Issues: (A) numeric values should not be written with more precision than is appropriate, (B)
     * vertical white space should not be wasted when printing arrays of numbers.
     */
    private static final Gson GSON_CONVERTER = createSharedGson();

    //These fields are marked transient so that they aren't included in the serialized JSON
    //The raw track data appears in the serialized from via the "String[] trackX" fields
    private final transient TrackPair rawTracks;
    @Nullable
    private final transient TrackPair smoothedTracks;

    private final String uniqueId;
    private final double eventScore;
    private final String eventDate;
    private final String eventTime;
    private final long eventEpochMsTime;
    private final double latitude;
    private final double longitude;
    private final long timeToCpaInMilliSec;

    private final Snapshot atEventTime;

    @org.apache.avro.reflect.Nullable
    private final Snapshot atEstimatedCpaTime;
    private final Snapshot atClosestLateral;
    @org.apache.avro.reflect.Nullable
    private final Snapshot atClosestLateralWith1kVert;
    @org.apache.avro.reflect.Nullable
    private final Snapshot atClosestVerticalWith3Nm;
    @org.apache.avro.reflect.Nullable
    private final Snapshot atClosestVerticalWith5Nm;

    private final boolean isLevelOffEvent;
    private final int courseDelta;
    private final ConflictAngle conflictAngle;

    private final SingleAircraftRecord aircraft_0;
    private final SingleAircraftRecord aircraft_1;

    @org.apache.avro.reflect.Nullable
    private final String[] track0;
    @org.apache.avro.reflect.Nullable
    private final String[] track1;
    private final String schemaVersion = SCHEMA_VERSION;

    @Nullable
    private final SerializableAnalysis airborneDynamics;

    private AirborneEvent(
        TrackPair rawTracks, 
        TrackPair smoothedTracks, 
        ScoredInstant event, 
        Snapshot[] importantMoments,
        SerializableAnalysis dynamics, 
        boolean publishTrackData
    ) {
        checkNotNull(rawTracks);
        checkNotNull(event);
        checkNotNull(importantMoments);
        checkArgument(importantMoments.length == 6);
        this.rawTracks = rawTracks;
        this.smoothedTracks = smoothedTracks;
        this.airborneDynamics = dynamics;

        PointPair points = bestAvailableData().interpolatedPointsAt(event.time());
        LatLong location = points.avgLatLong();

        this.eventScore = event.score();
        this.eventEpochMsTime = event.time().toEpochMilli();
        this.eventDate = utcDateAsString(event.time()); //eg "2020-02-20" "YYYY-MM-DD"
        this.eventTime = asZTimeString(event.time());  //eg "HH:mm:ss.SSS"
        this.latitude = location.latitude();
        this.longitude = location.longitude();

        Duration timeToCpa = points.closestPointOfApproach().timeUntilCpa();

        this.timeToCpaInMilliSec = timeToCpa.toMillis();
        this.atEventTime = importantMoments[0];
        this.atEstimatedCpaTime = importantMoments[1];
        this.atClosestLateral = importantMoments[2];
        this.atClosestLateralWith1kVert = importantMoments[3];
        this.atClosestVerticalWith3Nm = importantMoments[4];
        this.atClosestVerticalWith5Nm = importantMoments[5];
        this.isLevelOffEvent = isLevelOffEvent(event.time(), timeToCpa, bestAvailableData());
        this.courseDelta = (int) abs(Spherical.angleDifference(
            points.point1().course(),
            points.point2().course())
        );
        this.conflictAngle = ConflictAngle.beween(points.point1().course(), points.point2().course());

        //write down the raw track data...
        String[] trk0 = extractRawTrackData(rawTracks.track1());
        String[] trk1 = extractRawTrackData(rawTracks.track2());

        this.aircraft_0 = new SingleAircraftRecord(
            bestAvailableData().track1(),
            event.time(),
            hashForStringArray(trk0) //hashes are computed from raw data
        );
        this.aircraft_1 = new SingleAircraftRecord(
            bestAvailableData().track2(),
            event.time(),
            hashForStringArray(trk1) //hashes are computed from raw data
        );

        this.track0 = (publishTrackData) ? trk0 : null;
        this.track1 = (publishTrackData) ? trk1 : null;

        /*
         * This must be the last line in the constructor.  We compute the uuid by hashing the JSON
         * representation, therefore we cannot change the JSON after the uuid was set.
         */
        this.uniqueId = computeUniqueId();
    }

    AirborneEvent(TrackPair rawTracks, ScoredInstant event) {
        this(rawTracks, null, event, new Snapshot[6], null, false);
    }

    @Override
    public AirborneEvent event() {
        return this;
    }


    /**
     * A "Level off event" occurs when aircraft are predicted to have a very low future vertical
     * separation because one aircraft is climbing/descending into another aircraft's altitude
     * level. However, the climber/descender STOPS the climb/descent and begins flying level BEFORE
     * reaching the projected CPA.
     *
     * @param eventTime      When the minimum score was
     * @param timeToCpa      When the aircraft were projected to have their minimum separation
     * @param smoothedSource Smoothed Source data
     *
     * @return True of false
     */
    private static boolean isLevelOffEvent(Instant eventTime, Duration timeToCpa, TrackPair smoothedSource) {

        //the test we want to run doesn't work because "eventTime.plus(timeToCpa)" is outside the overlap)
        TimeWindow overlap = smoothedSource.timeOverlap().get();
        Instant approxCpaTime = eventTime.plus(timeToCpa);
        if (!overlap.contains(approxCpaTime)) {
            return false;
        }

        PointPair eventPoints = smoothedSource.interpolatedPointsAt(eventTime);
        PointPair cpaPoints = smoothedSource.interpolatedPointsAt(approxCpaTime);

        //when the
        Pair<Speed, Speed> earlyClimbRates = computeBothClimbRates(smoothedSource, eventTime, eventPoints);

        //did a climbing/descending aircraft level out?
        Pair<Speed, Speed> laterClimbRates = computeBothClimbRates(smoothedSource, approxCpaTime, cpaPoints);

        Speed laterClimbRate = null;

        if (abs(earlyClimbRates.first().inFeetPerMinutes()) > abs(earlyClimbRates.second().inFeetPerMinutes())) {
            //the first aircraft started with the biggest (in absolute value) climbrate...
            laterClimbRate = laterClimbRates.first(); //we want to know if he levels out
        } else {
            //the second aircraft has the biggest (in absolute value) climbrate...
            laterClimbRate = laterClimbRates.second();
        }
        //if the "primary altitude changer STOPPED changing altitude...we have a level off riskiestMoment
        return laterClimbRate.isZero();
    }

//    public Facility nopFacility() {
//        return this.facility;
//    }

    public TrackPair rawTracks() {
        return rawTracks;
    }

    public String callsign(int index) {
        return aircraft(index).callsign;
    }

    public String aircraftType(int index) {
        return aircraft(index).aircraftType;
    }

    public Distance altitude(int index) {
        return aircraft(index).altitude();
    }

    public IfrVfrStatus ifrVfrStatus(int index) {
        return aircraft(index).ifrVfrStatus();
    }

    public String climbStatus(int index) {
        return aircraft(index).climbStatus();
    }

    public Course course(int index) {
        return aircraft(index).course();
    }

    public Duration timeToCpa() {
        return Duration.ofMillis(timeToCpaInMilliSec);
    }

    public ConflictAngle conflictAngle() {
        return this.conflictAngle;
    }

    public int courseDelta() {
        return this.courseDelta;
    }

    public SingleAircraftRecord firstAircraft() {
        return aircraft_0;
    }

    public SingleAircraftRecord secondAircraft() {
        return aircraft_1;
    }

    public SingleAircraftRecord aircraft(int index) {
        if (index == 0) {
            return aircraft_0;
        }
        if (index == 1) {
            return aircraft_1;
        }

        throw new IllegalArgumentException("index can only be 0 or 1, not: " + index);
    }

    @Override
    public double score() {
        return this.eventScore;
    }

    public boolean isLevelOffEvent() {
        return isLevelOffEvent;
    }

    public PairedIfrVfrStatus pairedIfrVfrStatus() {
        return PairedIfrVfrStatus.from(aircraft_0.ifrVfrStatus(), aircraft_1.ifrVfrStatus());
    }

    @Override
    public LatLong latLong() {
        return LatLong.of(latitude, longitude);
    }

    public Distance avgAltitude() {
        if (aircraft_0 == null || aircraft_1 == null) {
            return null;
        }
        return mean(aircraft_0.altitude(), aircraft_1.altitude());
    }

    @Nullable
    public SerializableAnalysis airborneDynamics() {
        return airborneDynamics;
    }

    @Override
    public String asJson() {
        String asJson = GSON_CONVERTER.toJson(this);
        return HashUtils.removeArrayWhiteSpace(asJson); //do not let the arrays in the dynamics bork readability with too many newLine chars
    }

    private String computeUniqueId() {
        String asJson = GSON_CONVERTER.toJson(this);
        String uuid = hashForJson(asJson);
        return uuid;
    }

    public static AirborneEvent parseJson(String json) {
        return GSON_CONVERTER.fromJson(json, AirborneEvent.class);
    }

    public static AirborneEvent parse(Reader reader) {
        return GSON_CONVERTER.fromJson(reader, AirborneEvent.class);
    }

    public String uuid() {
        return this.uniqueId;
    }

    private static Pair<Speed, Speed> computeBothClimbRates(TrackPair sourceData, Instant eventTime, PointPair points) {

        return Pair.of(
            computeClimbRate(sourceData.track1(), eventTime),
            computeClimbRate(sourceData.track2(), eventTime)
        );
    }

    @Override
    public Instant time() {
        return Instant.ofEpochMilli(eventEpochMsTime);
    }

    public String zTime() {
        return this.eventDate + " " + this.eventTime;
    }

    public String eventDate() {
        return this.eventDate;
    }

    /** @return A String like "HH:mm:ss.SSS". */
    public String eventTimeOfDay() {
        return this.eventTime;
    }

    public boolean containsAircraftWithDataTag() {

        Point point1 = bestAvailableData().track1().nearestPoint(time());
        Point point2 = bestAvailableData().track2().nearestPoint(time());

        return point1.hasValidCallsign() || point2.hasValidCallsign();
    }

    public final TrackPair bestAvailableData() {
        return (smoothedTracks == null)
            ? rawTracks
            : smoothedTracks;
    }

    public String schemaVersion() {
        return this.schemaVersion;
    }

    public Snapshot snapshotAtEvent() {
        return atEventTime;
    }

    public Snapshot snapshotAtCpaTime() {
        return atEstimatedCpaTime;
    }

    public Snapshot snapshotAtClosestLateral() {
        return atClosestLateral;
    }

    public Snapshot snapshotAtClosestLateralWith1kVert() {
        return atClosestLateralWith1kVert;
    }

    public Snapshot snapshotAtClosestVerticalWith3NM() {
        return atClosestVerticalWith3Nm;
    }

    public Snapshot snapshotAtClosestVerticalWith5NM() {
        return atClosestVerticalWith5Nm;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.uniqueId);
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.eventScore) ^ (Double.doubleToLongBits(this.eventScore) >>> 32));
        hash = 29 * hash + Objects.hashCode(this.eventDate);
        hash = 29 * hash + Objects.hashCode(this.eventTime);
        hash = 29 * hash + (int) (this.eventEpochMsTime ^ (this.eventEpochMsTime >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.latitude) ^ (Double.doubleToLongBits(this.latitude) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.longitude) ^ (Double.doubleToLongBits(this.longitude) >>> 32));
        hash = 29 * hash + (int) (this.timeToCpaInMilliSec ^ (this.timeToCpaInMilliSec >>> 32));
        hash = 29 * hash + Objects.hashCode(this.atEventTime);
        hash = 29 * hash + Objects.hashCode(this.atEstimatedCpaTime);
        hash = 29 * hash + Objects.hashCode(this.atClosestLateral);
        hash = 29 * hash + Objects.hashCode(this.atClosestLateralWith1kVert);
        hash = 29 * hash + Objects.hashCode(this.atClosestVerticalWith3Nm);
        hash = 29 * hash + Objects.hashCode(this.atClosestVerticalWith5Nm);
        hash = 29 * hash + (this.isLevelOffEvent ? 1 : 0);
        hash = 29 * hash + this.courseDelta;
        hash = 29 * hash + Objects.hashCode(this.conflictAngle);
        hash = 29 * hash + Objects.hashCode(this.aircraft_0);
        hash = 29 * hash + Objects.hashCode(this.aircraft_1);
        hash = 29 * hash + Arrays.deepHashCode(this.track0);
        hash = 29 * hash + Arrays.deepHashCode(this.track1);
        hash = 29 * hash + Objects.hashCode(this.airborneDynamics);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AirborneEvent other = (AirborneEvent) obj;
        if (Double.doubleToLongBits(this.eventScore) != Double.doubleToLongBits(other.eventScore)) {
            return false;
        }
        if (this.eventEpochMsTime != other.eventEpochMsTime) {
            return false;
        }
        if (Double.doubleToLongBits(this.latitude) != Double.doubleToLongBits(other.latitude)) {
            return false;
        }
        if (Double.doubleToLongBits(this.longitude) != Double.doubleToLongBits(other.longitude)) {
            return false;
        }
        if (this.timeToCpaInMilliSec != other.timeToCpaInMilliSec) {
            return false;
        }
        if (this.isLevelOffEvent != other.isLevelOffEvent) {
            return false;
        }
        if (this.courseDelta != other.courseDelta) {
            return false;
        }
        if (!Objects.equals(this.uniqueId, other.uniqueId)) {
            return false;
        }
        if (!Objects.equals(this.eventDate, other.eventDate)) {
            return false;
        }
        if (!Objects.equals(this.eventTime, other.eventTime)) {
            return false;
        }
        if (!Objects.equals(this.atEventTime, other.atEventTime)) {
            return false;
        }
        if (!Objects.equals(this.atEstimatedCpaTime, other.atEstimatedCpaTime)) {
            return false;
        }
        if (!Objects.equals(this.atClosestLateral, other.atClosestLateral)) {
            return false;
        }
        if (!Objects.equals(this.atClosestLateralWith1kVert, other.atClosestLateralWith1kVert)) {
            return false;
        }
        if (!Objects.equals(this.atClosestVerticalWith3Nm, other.atClosestVerticalWith3Nm)) {
            return false;
        }
        if (!Objects.equals(this.atClosestVerticalWith5Nm, other.atClosestVerticalWith5Nm)) {
            return false;
        }
        if (this.conflictAngle != other.conflictAngle) {
            return false;
        }
        if (!Objects.equals(this.aircraft_0, other.aircraft_0)) {
            return false;
        }
        if (!Objects.equals(this.aircraft_1, other.aircraft_1)) {
            return false;
        }
        if (!Arrays.deepEquals(this.track0, other.track0)) {
            return false;
        }
        if (!Arrays.deepEquals(this.track1, other.track1)) {
            return false;
        }
        if (!Objects.equals(this.airborneDynamics, other.airborneDynamics)) {
            return false;
        }
        return true;
    }

    /**
     * How you might name a file that contains this riskiestMoment record.
     *
     * @return a String like "2019-11-29--76d4b281fe385000594a051467305e92"
     */
    public static String nameFile(JsonWritable anAirborneEventRecord) {
        AirborneEvent record = (AirborneEvent) anAirborneEventRecord;

        String date = utcDateAsString(record.time());

        return date + "--" + record.uuid();
    }

    /** Create a 5 digit "file suffix" that uses a timestamp. */
    private static String fileSuffix(Instant time) {
        int SEC_PER_DAY = 24 * 60 * 60;
        long daySeconds = time.getEpochSecond() % SEC_PER_DAY;

        return String.format("%05d", daySeconds);  //always use 5 characters for this number
    }

    private String[] extractRawTrackData(Track track1) {
        return track1.points().stream()
            .map(Point::asNop)
            .toArray(String[]::new);
    }

    static Builder newBuilder() {
        return new Builder();
    }

    static class Builder {

        TrackPair rawTracks;
        TrackPair smoothedTracks;
        ScoredInstant event;
        Snapshot[] importantMoments;
        @Nullable SerializableAnalysis dynamics;
        boolean publishTrackData;

        Builder rawTracks(TrackPair rawData) {
            this.rawTracks = rawData;
            return this;
        }

        Builder smoothedTracks(TrackPair smoothedData) {
            this.smoothedTracks = smoothedData;
            return this;
        }

        Builder riskiestMoment(ScoredInstant event) {
            this.event = event;
            return this;
        }

        Builder snapshots(Snapshot[] importantMoments) {
            this.importantMoments = importantMoments;
            return this;
        }

        Builder dynamics(SerializableAnalysis dynamics) {
            this.dynamics = dynamics;
            return this;
        }

        Builder includeTrackData(boolean publishTrackData) {
            this.publishTrackData = publishTrackData;
            return this;
        }

        AirborneEvent build() {
            return new AirborneEvent(rawTracks, smoothedTracks, event, importantMoments, dynamics, publishTrackData);
        }
    }

    /** Create a Gson configured to repress unneeded accuracy in double and float data. */
    private static Gson createSharedGson() {

        GsonBuilder builder = new GsonBuilder()
            .setPrettyPrinting()
            //Use reduced precision for SerializableAnalysis to reduce JSON size
            .registerTypeAdapter(SerializableAnalysis.class, new SerializableAnalysisSerializer());

        builder.registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
            DecimalFormat df = new DecimalFormat("#.#####");
            return new JsonPrimitive(Double.parseDouble(df.format(src)));
        });

        return builder.create();
    }

    //Create a custom JsonSerializer for SerializableAnalysis objects that represses unneeded accuracy
    private static class SerializableAnalysisSerializer implements JsonSerializer<SerializableAnalysis> {

        final Gson GSON_FOR_DYNAMICS = new GsonBuilder()
            .registerTypeAdapter(Float.class, (JsonSerializer<Float>) (src, typeOfSrc, context) -> {
                DecimalFormat df = new DecimalFormat("#.##");
                return new JsonPrimitive((float) Double.parseDouble(df.format(src)));
            })
            .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
                DecimalFormat df = new DecimalFormat("#.##");
                return new JsonPrimitive(Double.parseDouble(df.format(src)));
            })
            .create();

        @Override
        public JsonElement serialize(SerializableAnalysis t, Type type, JsonSerializationContext jsc) {
            return GSON_FOR_DYNAMICS.toJsonTree(t);
        }
    }
}
