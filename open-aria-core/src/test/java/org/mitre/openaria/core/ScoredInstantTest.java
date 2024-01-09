/*
 * NOTICE:
 * This is the copyright work of The MITRE Corporation, and was produced for the
 * U. S. Government under Contract Number DTFAWA-10-C-00080, and is subject to
 * Federal Aviation Administration Acquisition Management System Clause 3.5-13,
 * Rights In Data-General, Alt. III and Alt. IV (Oct. 1996).
 *
 * No other use other than that granted to the U. S. Government, or to those
 * acting on behalf of the U. S. Government, under that Clause is authorized
 * without the express written permission of The MITRE Corporation. For further
 * information, please contact The MITRE Corporation, Contracts Management
 * Office, 7515 Colshire Drive, McLean, VA  22102-7539, (703) 983-6000.
 *
 * Copyright 2021 The MITRE Corporation. All Rights Reserved.
 */

package org.mitre.openaria.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ScoredInstantTest {

    @Test
    public void testEqualsAndHashCode() {
        EqualsVerifier.forClass(ScoredInstant.class).verify();
    }

    @Test
    public void testJsonWritable() {
        ScoredInstant test = ScoredInstant.of(12.12, Instant.ofEpochMilli(1617025242521L));
        assertEquals(test, ScoredInstant.fromJson(test.asJson()));
    }
}