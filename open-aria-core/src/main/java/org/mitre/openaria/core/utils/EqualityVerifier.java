package org.mitre.openaria.core.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.function.Consumer;

/**
 * An EqualityVerifier ensures that every item it sees matches some unknown value
 */
public class EqualityVerifier<T> implements Consumer<T> {

    private final boolean permitsNull;

    private T mustMatchThisValue;

    public EqualityVerifier() {
        this(false);

    }

    public EqualityVerifier(boolean permitsNull) {
        this.permitsNull = permitsNull;
    }

    /* Once this is true all inputs received via the accept() method must match. */
    private boolean hasValueToMatch() {
        return nonNull(mustMatchThisValue);
    }

    /**
     * @return the item T all inputs via accept must match.  Return null if no "imprintable" input
     *     has been received yet.
     */
    public T imprintedValue() {
        return mustMatchThisValue;
    }

    @Override
    public void accept(T t) {

        if (isNull(t)) {
            if (permitsNull) {
                return;
            } else {
                throw new IllegalArgumentException("Null is an illegal input");
            }
        }

        if (hasValueToMatch()) {
            checkArgument(t.equals(mustMatchThisValue));
        } else {
            mustMatchThisValue = t;
        }
    }
}
