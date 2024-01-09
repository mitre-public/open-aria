
package org.mitre.openaria.core;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Set;

public class TestUtils {

    /**
     * Confirm (1) that the number Points in the actualResult equals the number of Strings in the
     * expected points array, and (2) that every Point in the actual results corresponds to a String
     * in the expected Points array (using p.asNop()).
     * <p>
     * This method is used to write unit tests that verify an expected outcome set.
     *
     * @param actualResults  A Collection of points (typically generated algorithmically)
     * @param expectedPoints An array of NOP Strings
     */
    public static void confirmNopEquality(Collection<Point> actualResults, String... expectedPoints) {

        assertEquals(
            actualResults.size(), expectedPoints.length,
            "The number of actual results must match the number of expected Points"
        );

        Set<String> arrayOfPoints = newHashSet(expectedPoints);

        for (Point actualResult : actualResults) {
            assertTrue(
                arrayOfPoints.contains(actualResult.asNop()),
                "The actualResult:\n" + actualResult.asNop() + " \nwas not found in the array of expected points"
            );
        }
    }

}
