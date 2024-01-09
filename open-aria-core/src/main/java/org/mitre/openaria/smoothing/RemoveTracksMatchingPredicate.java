package org.mitre.openaria.smoothing;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataFilter;


/**
 * The {@code RemoveTracksMatchingPredicate} "cleans/smooths" tracks by simply removing them when
 * they match the provided predicate. If an {@code onRemoval} consumer is provided, the rejected
 * tracks will be routed there.
 * <p>
 * This is essentially a negated version of {@link DataFilter}, but the naming and structure
 * provides clarity to avoid confusion about what happens when a predicate = True (in this class, it
 * means tracks will be removed).
 */
public class RemoveTracksMatchingPredicate<T extends Track> extends DataFilter<T> {

    public RemoveTracksMatchingPredicate(Predicate<T> test) {
        // Intentionally NOT negated here; only in the "master" constructor.
        this(test, ignored -> {});
    }

    public RemoveTracksMatchingPredicate(Predicate<T> test, Consumer<T> onRemoval) {
        // THIS NEGATE IS IMPORTANT! ALL CONSTRUCTORS SHOULD BE ROUTED THROUGH HERE
        super(test.negate(), onRemoval);
    }

}
