package org.mitre.openaria.airborne;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import static java.util.Objects.nonNull;

import org.junit.jupiter.api.Test;

public class TowerFacilityPairTest {
    
    @Test
    public void towerFacilityPairingIsNotNull() {

        String towerString = "VGT";
        TowerFacilityPair pair = TowerFacilityPair.fromString(towerString);

        assertTrue(nonNull(pair));

    }

    @Test
    public void towerFacilityPairingIsNull() {
        String towerString = "ROA";
        TowerFacilityPair pair = TowerFacilityPair.fromString(towerString);

        assertFalse(nonNull(pair));
    }

    @Test
    public void getFacilityForTowerPairing() {
        String towerString = "VGT";
        TowerFacilityPair pair = TowerFacilityPair.fromString(towerString);

        assertThat(pair.getFacilityWithBestView().toString(), equalTo("LAS"));
    }

}
