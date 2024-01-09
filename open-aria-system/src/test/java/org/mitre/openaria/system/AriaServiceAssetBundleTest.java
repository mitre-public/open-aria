
package org.mitre.openaria.system;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AriaServiceAssetBundleTest {

    @Test
    public void numWorkerThreadsMustBeAtLeast1() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AriaServiceAssetBundle(0)
        );
    }

    @Test
    public void twoDistinctExecutorsAreProvided() {

        AriaServiceAssetBundle bundle = new AriaServiceAssetBundle(12);
        assertThat(bundle.mainExecutor(), not(sameInstance(bundle.fastTaskExecutor())));
    }
}
