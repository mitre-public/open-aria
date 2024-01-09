
package org.mitre.openaria.core;

/**
 * A MutablePoint is a Point that can be manipulated.
 * <p>
 * Generally speaking, mutable data should be avoided if possible (see also Item 15 from Joshua
 * Block's Effective Java). Consequently, classes that implement this interface should be avoided
 * when possible.
 */
public interface MutablePoint extends Point {

    /**
     * Set a field of this Point.
     *
     * @param field The Field to set
     * @param value The value to set
     */
    public void set(PointField field, Object value);
}
