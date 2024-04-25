package org.mitre.openaria.core;

import org.mitre.caasd.commons.parsing.nop.Facility;

/**
 * This class contains convenience methods that perform tasks that are unique to the ARIA system.
 */
public class AriaUtils {

    /**
     * Return true if it appears that a raw data file contains data from a specific Facility. This
     * determination is merely based on the filename, not the actual content of a file. This method
     * will recognize filenames like: "A80_20160712_151254_d8a68479-29b4-49d7-b449-5a5fc5f453ba.A80"
     * (which are files from the raw NOP data feed provided by the FAA) or
     * "STARS_A80_RH_20161018.txt.gz". (which are files available from: ml-gw01:/dmc3/asias/nop/)
     *
     * @param filename The name of a raw data file
     * @param facility A facility
     *
     * @return True, if and only if the name of the raw file "matches" one of the two expected
     *     filename formats.
     */
    public static boolean fileIsFromFacility(String filename, Facility facility) {

        /*
         * MATCH against file names like:
         * "A80_20160712_151254_d8a68479-29b4-49d7-b449-5a5fc5f453ba.A80"
         *
         * File names like this came from the raw NOP data sample provided by the FAA
         */
        if (filename.startsWith(facility.toString())) {
            return true;
        }

        /*
         * MATCH against file names like: "STARS_A80_RH_20161018.txt.gz"
         *
         * File names like this come from raw NOP data downloaded from: ml-gw01:/dmc3/asias/nop/
         */
        boolean facilityComesAfterPrefix
            = filename.startsWith("ARTS_" + facility) //agw
            || filename.startsWith("CENTER_" + facility)
            || filename.startsWith("STARS_" + facility)
            || filename.startsWith("MEARTS_" + facility);

        return facilityComesAfterPrefix;
    }

    public static Facility getFacilityFromFilename(String filename) {

        for (Facility facility : Facility.values()) {
            if (fileIsFromFacility(filename, facility)) {
                return facility;
            }
        }
        return null;
    }

}
