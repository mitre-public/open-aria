package org.mitre.openaria.trackpairing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mitre.openaria.trackpairing.IsFormationFlight.parseFormationFilterDefinition;
import static org.mitre.openaria.trackpairing.IsFormationFlight.parseMultipleFilterDefs;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.trackpairing.IsFormationFlight.FormationFilterDefinition;
import org.mitre.caasd.commons.Distance;

public class IsFormationFlightTest {

    @Test
    public void canParseOneDefinition() {
        String singleDef = "0.85,92,true";

        FormationFilterDefinition def = parseFormationFilterDefinition(singleDef);

        assertThat(def.timeRequirement, is(Duration.ofSeconds(92)));
        assertThat(def.proximityRequirement, is(Distance.ofNauticalMiles(0.85)));
        assertThat(def.logRemovedFilter, is(true));
    }

    @Test
    public void canParseMultipleDefinitions() {

        String twoDefs = "0.85,92,true|0.5,61,false";

        List<FormationFilterDefinition> defs = parseMultipleFilterDefs(twoDefs);

        assertThat(defs, hasSize(2));

        assertThat(defs.get(0).timeRequirement, is(Duration.ofSeconds(92)));
        assertThat(defs.get(0).proximityRequirement, is(Distance.ofNauticalMiles(0.85)));
        assertThat(defs.get(0).logRemovedFilter, is(true));

        assertThat(defs.get(1).timeRequirement, is(Duration.ofSeconds(61)));
        assertThat(defs.get(1).proximityRequirement, is(Distance.ofNauticalMiles(0.50)));
        assertThat(defs.get(1).logRemovedFilter, is(false));
    }
}
