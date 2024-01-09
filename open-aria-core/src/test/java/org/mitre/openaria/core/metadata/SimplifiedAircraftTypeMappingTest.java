
package org.mitre.openaria.core.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mitre.caasd.commons.actype.models.AircraftType;

public class SimplifiedAircraftTypeMappingTest {

    private SimplifiedAircraftTypeMapping instance() {
        return new SimplifiedAircraftTypeMapping();
    }

    @Test
    public void testFromDesignator() {
        AircraftType actual = instance().fromDesignator("B738");
        assertNotNull(actual, "Must return actual AircraftType");
        assertEquals("B738", actual.getTypeDesignator());
    }

    @Test
    public void testBucketedAircraftClass_FromString() {
        String actual = instance().bucketedAircraftClass("A320");
        assertEquals("FIXED_WING", actual);
    }

    @Test
    public void bucketedAircraftClass_String_nullInputResultsInUnknown() {
        String input = null;
        String actual = instance().bucketedAircraftClass(input);
        assertEquals("UNKNOWN", actual);
    }

    @Test
    public void testBucketedAircraftClass() {
        SimplifiedAircraftTypeMapping test = new SimplifiedAircraftTypeMapping();
        AircraftType A1 = test.fromDesignator("A129");
        assertEquals("ROTORCRAFT", test.bucketedAircraftClass(A1));
    }

    @Test
    public void bucketedAircraftClass_AircraftType_nullInputResultsInUnknown() {
        AircraftType input = null;
        String actual = instance().bucketedAircraftClass(input);
        assertEquals("UNKNOWN", actual);
    }

    @Test
    public void testBucketedEngineType_FromString() {

        String actual = instance().bucketedEngineType("F406");
        assertEquals("TURBOPROP", actual);
    }

    @Test
    public void testBucketedEngineType() {

        SimplifiedAircraftTypeMapping test = new SimplifiedAircraftTypeMapping();
        AircraftType rocket = test.fromDesignator("ROAR");
        assertEquals("OTHER", test.bucketedEngineType(rocket));
    }

    @Test
    public void testBucketedPilotSystem_FromString() {
        String actual = instance().bucketedPilotSystem("MQ9");
        assertEquals("NOT_MANNED", actual);
    }

    @Test
    public void testBucketedPilotSystem() {
        SimplifiedAircraftTypeMapping test = new SimplifiedAircraftTypeMapping();
        AircraftType dc9 = test.fromDesignator("DC9");
        assertEquals("MANNED", test.bucketedPilotSystem(dc9));
    }

    @Test
    public void testIsMilitary() {

        SimplifiedAircraftTypeMapping test = new SimplifiedAircraftTypeMapping();
        AircraftType e = test.fromDesignator("EDGE");
        assertEquals("FALSE", test.isMilitary(e));
    }

    @Test
    public void testFromDesignator_FromUnknown() {

        assertNull(
            instance().fromDesignator("alsdjfklasjdf"),
            "Must return null when designator not recognized"
        );

        assertNull(
            instance().fromDesignator(null),
            "Must return null when designator is null"
        );
    }

    @Test
    public void testNullSafety() {

        SimplifiedAircraftTypeMapping test = instance();

        assertEquals("UNKNOWN", test.bucketedAircraftClass((String) null));
        assertEquals("UNKNOWN", test.bucketedAircraftClass((AircraftType) null));

        assertEquals("UNKNOWN", test.bucketedEngineType((String) null));
        assertEquals("UNKNOWN", test.bucketedEngineType((AircraftType) null));

        assertEquals("UNKNOWN", test.bucketedPilotSystem((String) null));
        assertEquals("UNKNOWN", test.bucketedPilotSystem((AircraftType) null));

        assertEquals("UNKNOWN", test.isMilitary((String) null));
        assertEquals("UNKNOWN", test.isMilitary((AircraftType) null));
    }

}
