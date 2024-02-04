
package org.mitre.openaria.system;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

public class DirectoryCleanerTest {

    static final Consumer<File> NO_OP_CONSUMER = (File f) -> {};

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
