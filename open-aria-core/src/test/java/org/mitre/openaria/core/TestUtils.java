
package org.mitre.openaria.core;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Set;

import org.mitre.openaria.core.formats.nop.NopEncoder;
import org.mitre.openaria.core.formats.nop.NopHit;

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
    public static void confirmNopEquality(Collection<Point<NopHit>> actualResults, String... expectedPoints) {

        assertEquals(
            actualResults.size(), expectedPoints.length,
            "The number of actual results must match the number of expected Points"
        );

        Set<String> arrayOfPoints = newHashSet(expectedPoints);

        NopEncoder nopEncoder = new NopEncoder();

        for (Point<NopHit> actualResult : actualResults) {
            String asNop = nopEncoder.asRawNop(actualResult);
            assertThat(arrayOfPoints.contains(asNop), is(true));
        }
    }

}
