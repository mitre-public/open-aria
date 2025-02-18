package org.mitre.openaria.airborne;

import static java.time.Instant.EPOCH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.openaria.airborne.AirborneEvent.newBuilder;
import static org.mitre.openaria.airborne.AirborneEvent.parseJson;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseNopTime;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.time.Duration;
import java.time.Instant;

import org.mitre.openaria.core.ScoredInstant;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.Formats;
import org.mitre.openaria.core.formats.nop.NopHit;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class AirborneEventTest {

    /** Use of pretty printing in default {@link Gson} messes up the test literals */
    static Gson gson = new Gson();

    static TrackPair<NopHit> SCARY_TRACK_PAIR = makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"));

    static ScoredInstant SCORED_INSTANT = new ScoredInstant(
        3.0,
        parseNopTime("03/24/2018", "15:02:59.117")
    );

    @Test
    public void toJson_fromJson_cycleIsConsistent() {
        AirborneEvent record = new AirborneEvent(SCARY_TRACK_PAIR, Formats.nop(), SCORED_INSTANT);

        String json = record.asJson();

        AirborneEvent record_round2 = parseJson(json);

        String json2 = record_round2.asJson();

        assertThat(json, is(json2));
    }

    //simple utility method to make a test EventRecord with a known time, facility, and score
    public static AirborneEvent testRecord(Instant time, double score) {

        //manually build the JSON then abuse the "JsonWritable-ness" of AirborneEvent to get a test Record
        // ick...serialization layer abuse
        String json = "{\n"
            + "  \"eventEpochMsTime\": " + time.toEpochMilli() + ",\n"
            + "  \"eventScore\": " + score + "\n"
            + "}";

        return AirborneEvent.parseJson(json);
    }

    @Test
    public void testRecord_worksCorrectly() {
        AirborneEvent mockRecord = testRecord(
            Instant.EPOCH.plus(Duration.ofDays(12)),
            123.0
        );

        assertThat(mockRecord.time(), is(EPOCH.plus(Duration.ofDays(12))));
        assertThat(mockRecord.score(), is(123.0));
    }

    @Test
    public void Equals_Correct() {
        EqualsVerifier.forClass(AirborneEvent.class)
            .withIgnoredFields("schemaVersion")
            .verify();
    }

    @Test
    public void ToJson_WithAirborneDynamics_Correct() {
        AirborneEvent record = newBuilder()
            .rawTracks(SCARY_TRACK_PAIR)
            .format(Formats.nop())
            .riskiestMoment(SCORED_INSTANT)
            .snapshots(new Snapshot[]{null, null, null, null, null, null})
            .dynamics(AirborneDynamicsTest.asObj)
            .build();

        String json = gson.toJson(record);

        assertThat(json, containsString(nestedAirborneDynamicsJsonLiteral()));
    }

    @Test
    public void ToJson_WithoutAirborneDynamics_Correct() {
        AirborneEvent record = new AirborneEvent(SCARY_TRACK_PAIR, Formats.nop(), SCORED_INSTANT);

        String json = gson.toJson(record);

        assertThat(json, not(containsString(nestedAirborneDynamicsJsonLiteral())));
    }

    @Test
    public void uuidInJsonSameAsUuidFromMethod() {
        AirborneEvent record = new AirborneEvent(SCARY_TRACK_PAIR, Formats.nop(), SCORED_INSTANT);

        String uuidFromApi = record.uuid();
        String hashFromJson = new JsonParser().parse(record.asJson())
            .getAsJsonObject()
            .getAsJsonPrimitive("uniqueId")  //HashUtils.HASH_FIELD_NAME = "uniqueId"
            .getAsString();

        assertThat(uuidFromApi, is(hashFromJson));
    }

    private static String nestedAirborneDynamicsJsonLiteral() {
        return "\"airborneDynamics\":" + AirborneDynamicsTest.asStandardJson;
    }
}
