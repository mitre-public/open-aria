package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.airborne.SerializableAnalysis.checkSameLength;
import static org.mitre.openaria.airborne.SerializableAnalysis.parseJson;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import nl.jqno.equalsverifier.EqualsVerifier;

public class AirborneDynamicsTest {

    /**
     * Use of pretty printing in default {@link Gson} messes up the test literals
     */
    static Gson gson = new Gson();

    public static final SerializableAnalysis asObj = new SerializableAnalysis(
        new long[]{0L, 1L},
        new float[]{0.0F, 1.0F},
        new float[]{2.0F, 3.0F},
        new long[]{2L, 3L},
        new float[]{4.0F, 5.0F},
        new float[]{6.0F, 7.0F},
        new double[]{0.0D, 1.0D}
    );

    public static final String asPrettyJson = "{\n"
        + "  \"epochMsTime\": [\n"
        + "    0,\n"
        + "    1\n"
        + "  ],\n"
        + "  \"trueVerticalFt\": [\n"
        + "    2.0,\n"
        + "    3.0\n"
        + "  ],\n"
        + "  \"trueLateralNm\": [\n"
        + "    0.0,\n"
        + "    1.0\n"
        + "  ],\n"
        + "  \"estTimeToCpaMs\": [\n"
        + "    2,\n"
        + "    3\n"
        + "  ],\n"
        + "  \"estVerticalAtCpaFt\": [\n"
        + "    6.0,\n"
        + "    7.0\n"
        + "  ],\n"
        + "  \"estLateralAtCpaNm\": [\n"
        + "    4.0,\n"
        + "    5.0\n"
        + "  ],\n"

        + "  \"score\": [\n"
        + "    0.0,\n"
        + "    1.0\n"
        + "  ]\n"
        + "}";

    public static final String asStandardJson = "{\"epochMsTime\":[0,1],\"trueVerticalFt\":[2.0,3.0],\"trueLateralNm\":[0.0,1.0],\"estTimeToCpaMs\":[2,3],\"estVerticalAtCpaFt\":[6.0,7.0],\"estLateralAtCpaNm\":[4.0,5.0],\"score\":[0.0,1.0]}";

    @Test
    public void Equals_Correct() {
        EqualsVerifier.forClass(SerializableAnalysis.class)
            .verify();
    }

    @Test
    public void CheckSameLength_SameLengths_NoProblem() {
        checkSameLength(new Object[10], new Long[10], new Double[10]);
    }

    @Test
    public void CheckSameLength_DifferentLengths_Error() {
        assertThrows(
            IllegalArgumentException.class,
            () ->  checkSameLength(new Object[1], new Long[10], new Double[10])
        );
    }

    @Test
    public void toJson_fromJson_cycleIsConsistent() {
        SerializableAnalysis record = asObj;

        String json = record.asJson();

        // cache for use in other tests
//		System.out.println(json);
//		System.out.println(gson.toJson(record)); // not-pretty print as of writing
        SerializableAnalysis record_round2 = parseJson(json);

        String json2 = record_round2.asJson();

        assertThat(json, is(json2));
    }

    @Test
    public void toJson_fromJson_regression() {
        SerializableAnalysis record = asObj;

        String actual = record.asJson();

        assertThat(actual, is(asPrettyJson));
    }

}
