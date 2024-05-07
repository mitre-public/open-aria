package org.mitre.openaria.kafka;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.mitre.caasd.commons.util.PropertyUtils.getOptionalInt;
import static org.mitre.caasd.commons.util.PropertyUtils.loadProperties;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.mitre.openaria.core.formats.nop.Facility;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * This class Pairs each Facility with a unique non-negative integer. This integer is used to
 * identify a Kafka Partition where a specific facility's data can be found. For example, Facility
 * A11's data is stored in partition 0 and Facility A80's data is stored in partition 1.
 * <p>
 * This FacilityPartitionMapping mapping is loaded from a configuration file.
 */
public class FacilityPartitionMapping implements PartitionMapping<Facility> {

    private final BiMap<Facility, Integer> facilityToPartitionMapping;

    /**
     * @param props A flat text file that contains a sequence of lines like: <br> "A11 : 0" <br>
     *              "A80 : 1" <br> "A90 : 2" <br> "ABE : 3" <br> , ... etc.
     */
    public FacilityPartitionMapping(Properties props) {
        this.facilityToPartitionMapping = makeMapping(props);
    }

    public static FacilityPartitionMapping parseFacilityMappingFile(File file) {
        return new FacilityPartitionMapping(loadProperties(file));
    }

    private BiMap<Facility, Integer> makeMapping(Properties props) {

        HashMap<Facility, Integer> map = new HashMap<>();

        for (Facility facility : Facility.values()) {
            int partitionNumber = getOptionalInt(facility.toString(), props, -1);
            if (partitionNumber >= 0) {
                map.put(facility, partitionNumber);
            }
        }

        checkState(map.size() == map.values().size());

        return HashBiMap.create(map);
    }

    /**
     * Kafka topics have 1 partition for each facility. This method looks up the partition number
     * for a given Facility
     *
     * @param facility A facility.
     *
     * @return The partition number assigned to this Facility
     */
    @Override
    public Optional<Integer> partitionFor(Facility facility) {
        return Optional.ofNullable(facilityToPartitionMapping.get(facility));
    }

    public boolean hasPartitionFor(Facility facility) {
        return facilityToPartitionMapping.containsKey(facility);
    }

    @Override
    public Optional<Facility> itemForPartition(int partition) {
        return facilityFor(partition);
    }

    /**
     * Kafka topics have 1 partition for each facility. This method looks up the Facility for a
     * given partition number.
     *
     * @param partitionNumber A partition number.
     *
     * @return The Facility assigned to the partition number given.
     */
    public Optional<Facility> facilityFor(int partitionNumber) {
        return Optional.ofNullable(facilityToPartitionMapping.inverse().get(partitionNumber));
    }

    @Override
    public List<Facility> partitionList() {
        return newArrayList(Facility.values());
    }
}
