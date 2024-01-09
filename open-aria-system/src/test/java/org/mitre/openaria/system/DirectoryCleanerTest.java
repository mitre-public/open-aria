
package org.mitre.openaria.system;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.Functions.NO_OP_CONSUMER;

import java.io.File;

import org.junit.jupiter.api.Test;

public class DirectoryCleanerTest {

    @Test
    public void confirmMissingDirectoryThrowsIaeAtConstruction() {
        //prevent creating a DirectoryCleaner that has no directory to clean
        assertThrows(
            IllegalArgumentException.class,
            () -> new DirectoryCleaner("thisDirDoesntExist", NO_OP_CONSUMER)
        );
    }

    @Test
    public void confirmNullDirectoryThrowsNpeAtConstruction() {
        //prevent creating a DirectoryCleaner that has no directory to clean
        assertThrows(
            NullPointerException.class,
            () -> new DirectoryCleaner(null, NO_OP_CONSUMER)
        );
    }

    @Test
    public void confirmNullConsumerThrowsNpeAtConstruction() {

        String DIR_NAME = "deleteThisTestDir";
        File dir = new File(DIR_NAME);

        assertThrows(
            NullPointerException.class,
            () -> new DirectoryCleaner(DIR_NAME, null)
        );
    }

}
