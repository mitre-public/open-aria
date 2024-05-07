
package org.mitre.openaria.system;

import static com.google.common.collect.Sets.newHashSet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.openaria.core.formats.nop.Facility;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;

/**
 * A FacilitySet represents an Immutable set of Facilties. A FacilitySet is typically used as an
 * input parameter that tells pieces of the ARIA System which facilities it should process.
 * <p>
 * This class was added to (A) make it more convenient to build FacilitySets from properties files,
 * (B) guarantee the set is immutable, and (C) make client code easier to read.
 */
public class FacilitySet implements Iterable<Facility> {

    private final ImmutableSet<Facility> facilities;

    /**
     * Create a FacilitySet from a set of Properties. Each facility can have it's property set to
     * "ON" or "OFF" (not case sensitive). If a facility does not have a corresponding value that
     * facility is set to "OFF" meaning that the facility is not included in the set.
     *
     * @param props A set of properties.
     *
     * @return A FacilitySet reflecting the properties provided
     */
    public static FacilitySet from(Properties props) {

        Set<Facility> set = new HashSet<>();

        for (Facility facility : Facility.values()) {

            String onOrOff = props.getProperty(
                facility.name(),
                "OFF" //default value when facility is missing
            );

            if (onOrOff.equalsIgnoreCase("ON")) {
                set.add(facility);
            } else if (onOrOff.equalsIgnoreCase("OFF")) {
                //do nothing
            } else {
                throw new IllegalArgumentException(
                    "The property " + onOrOff + " for facility " + facility
                        + " is invalid.  The only valid values are \"ON\" or \"OFF\"."
                );
            }
        }

        return new FacilitySet(set);
    }

    /**
     * Create a FacilitySet from the Properties found in a particular file. Each facility can have
     * it's property set to "ON" or "OFF" (not case sensitive). If a facility does not have a
     * corresponding value that facility is set to "OFF" meaning that the facility is not included
     * in the set.
     *
     * @param filename A properties file
     *
     * @return A FacilitySet reflecting the properties provided
     */
    public static FacilitySet from(String filename) {
        try {
            Properties props = FileUtils.getProperties(new File(filename));

            return from(props);
        } catch (IOException ioe) {
            throw new IllegalStateException(
                "Could not load the property file: " + filename,
                ioe
            );
        }
    }

    /**
     * @return A FacilitySet that includes all known Facilities.
     */
    public static FacilitySet allFacilites() {
        return new FacilitySet(newHashSet(Facility.values()));
    }

    public int size() {
        return this.facilities.size();
    }

    public boolean isEmpty() {
        return this.facilities.isEmpty();
    }

    /**
     * Create a FacilitySet (which is Immutable) from a Set<Facility> which could be mutable.
     *
     * @param facilities A set of Facilities
     */
    public FacilitySet(Set<Facility> facilities) {
        this.facilities = new ImmutableSet.Builder().addAll(facilities).build();
    }

    public boolean includes(Facility facility) {
        return facilities.contains(facility);
    }

    @Override
    public UnmodifiableIterator<Facility> iterator() {
        return facilities.iterator();
    }
}
