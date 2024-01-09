
package org.mitre.openaria.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class FacilitySetTest {


    @Test
    public void testWellFormedFacilitySetFile() throws Exception {

        String TEST_FILE = "testFacilitySet.properties";

        Optional<File> file = FileUtils.getResourceAsFile(FacilitySetTest.class, TEST_FILE);

        assertTrue(file.isPresent());

        FacilitySet facilities = FacilitySet.from(
            file.get().getAbsolutePath()
        );

        assertTrue(facilities.includes(Facility.A11), "Facilities that are ON should be included");
        assertTrue(facilities.includes(Facility.A80), "Facilities that are ON should be included");
        assertTrue(facilities.includes(Facility.A90), "Facilities that are ON should be included");
        assertFalse(facilities.includes(Facility.ABE), "Facilities that are OFF should not be included");
        assertFalse(facilities.includes(Facility.ABI), "Facilities that are OFF should not be included");
        assertFalse(facilities.includes(Facility.ABQ), "Facilities that are OFF should not be included");
        assertFalse(facilities.includes(Facility.ZAB), "Missing Facilities are not included");
    }

    @Test
    public void testInvalidValueInFacilitySetFile() throws Exception {
        /*
         * Ensure we prohibit facility set properties files that contain bad values.
         */

        String TEST_FILE = "badFacilitySet.properties";

        Optional<File> file = FileUtils.getResourceAsFile(FacilitySetTest.class, TEST_FILE);

        assertTrue(file.isPresent());

        assertThrows(
            IllegalArgumentException.class,
            () -> FacilitySet.from(file.get().getAbsolutePath()),
            "Creating a facility set from an improperly formatted file should fail"
        );
    }
}
