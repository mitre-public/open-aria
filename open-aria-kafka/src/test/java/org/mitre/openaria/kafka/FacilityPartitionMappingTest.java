package org.mitre.openaria.kafka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class FacilityPartitionMappingTest {

    public FacilityPartitionMapping sampleMapping() {
        File file = getResourceFile("facilityToPartition.properties");

        return new FacilityPartitionMapping(loadProperties(file));
    }

    @Test
    public void partitionForWorksForEveryFacility() {

        FacilityPartitionMapping mapping = sampleMapping();

        for (Facility facility : Facility.values()) {
            Optional<Integer> partition = mapping.partitionFor(facility);

            assertTrue(partition.get() >= 0, "All partition numbers should be non-negative");
        }
    }

    @Test
    public void facilityForWorks() {

        FacilityPartitionMapping mapping = sampleMapping();

        for (Facility facility : Facility.values()) {

            Optional<Integer> partition = mapping.partitionFor(facility);
            Optional<Facility> facilityFromPartition = mapping.facilityFor(partition.get());

            assertEquals(facility, facilityFromPartition.get());
        }
    }

    @Test
    public void canMakingMappingFromFileWithPartialFacilityList() {
        File file = getResourceFile("eimFacilityPartitionMapping.properties");

        assertDoesNotThrow(
            () ->  new FacilityPartitionMapping(loadProperties(file))
        );
    }

    @Test
    public void partialFacilityListCanBeQueried() {

        File file = getResourceFile("eimFacilityPartitionMapping.properties");
        FacilityPartitionMapping mapping = new FacilityPartitionMapping(loadProperties(file));

        assertThat(mapping.hasPartitionFor(Facility.PVD), is(false));
        assertThat(mapping.hasPartitionFor(Facility.A80), is(true));
    }
}
