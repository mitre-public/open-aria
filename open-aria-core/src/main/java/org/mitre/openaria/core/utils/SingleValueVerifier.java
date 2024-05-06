package org.mitre.openaria.core.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.function.Consumer;

/**
 * A SingleValueVerifier ensures that every item it sees matches some unknown value
 */
public class SingleValueVerifier<T> implements Consumer<T> {

    // When this is false ANY null provided to "accept" produces and IllegalArgumentException
    private final boolean permitsNull;

    private T mustMatchThisValue;

    /** Create a SingleValueVerifier that does not tolerate Null Values. */
    public SingleValueVerifier() {
        this(false);
    }

    /**
     * Create a SingleValueVerifier that may or may not tolerate Null Values.
     *
     * @param permitsNull When true: ignore null values completely, When false: throw an
     *                    IllegalArgumentException anytime a null value is received via "accept"
     */
    public SingleValueVerifier(boolean permitsNull) {
        this.permitsNull = permitsNull;
    }

    /**
     * @return The item T all inputs via accept must match.  Return null if no "imprintable" input
     *     has been received yet.
     */
    public T imprintedValue() {
        return mustMatchThisValue;
    }

    /**
     * Receive a value t, verify this particular value t matches every other t ever received in
     * prior calls to "accept(t)".
     */
    @Override
    public void accept(T t) {

        if (isNull(t) && permitsNull) {
            return;
        }

        if (isNull(t) && !permitsNull) {
            throw new IllegalArgumentException("Null is an illegal input");
        }

        if (hasValueToMatch()) {
            checkArgument(t.equals(mustMatchThisValue));
        } else {
            mustMatchThisValue = t;
        }
    }

    /* Once this is true all inputs received via the accept() method must match. */
    private boolean hasValueToMatch() {
        return nonNull(mustMatchThisValue);
    }
}
