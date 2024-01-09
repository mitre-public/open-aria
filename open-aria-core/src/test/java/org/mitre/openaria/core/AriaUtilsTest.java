
package org.mitre.openaria.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mitre.openaria.core.AriaUtils.fileIsFromFacility;
import static org.mitre.openaria.core.AriaUtils.getFacilityFromFilename;

import org.mitre.caasd.commons.parsing.nop.Facility;

public class AriaUtilsTest {

    @Test
    public void testFileIsFromFacility_format1() {
        /*
         * Confirm Facilities.fileIsFromFacility properly recognizes filenames with this formating
         */
        String filename = "A80_20160712_151231_c7d111cc-011c-4bd7-8623-2bd771fea801.A80";
        Facility correctFacility = Facility.A80;
        Facility wrongFacility = Facility.N90;

        assertTrue(fileIsFromFacility(filename, correctFacility));
        assertFalse(fileIsFromFacility(filename, wrongFacility));
    }

    @Test
    public void testFileIsFromFacility_format2() {
        /*
         * Confirm Facilities.fileIsFromFacility properly recognizes filenames with this formating
         */
        String filename = "STARS_CMH_RH_20161018.txt.gz";
        Facility correctFacility = Facility.CMH;
        Facility wrongFacility = Facility.A11;

        assertTrue(fileIsFromFacility(filename, correctFacility));
        assertFalse(fileIsFromFacility(filename, wrongFacility));
    }

    @Test
    public void testGetFacility_format1() {
        /*
         * Confirm Facilities.getFacility can extract the proper Facility given this filename
         */
        String filename = "A80_20160712_151231_c7d111cc-011c-4bd7-8623-2bd771fea801.A80";

        assertEquals(
            Facility.A80,
            getFacilityFromFilename(filename)
        );
    }

    @Test
    public void testGetFacility_format2() {
        /*
         * Confirm Facilities.getFacility can extract the proper Facility given this filename
         */
        String filename = "STARS_CMH_RH_20161018.txt.gz";

        assertEquals(
            Facility.CMH,
            getFacilityFromFilename(filename)
        );
    }

}
