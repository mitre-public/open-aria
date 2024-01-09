
package org.mitre.openaria.core;

import java.util.Collection;

/**
 * A EventDetector converts a TrackPair to a Collection of "Events" of type T.
 * <p>
 * This interface is designed to support the composition of EventDetectors and event filters into
 * powerful, and flexible, Event Detection capabilities. In other words, the initial implementation
 * of EventDetector should return the largest possible Collection of events. This way a separation
 * event filtering class or operation can narrow the collection of events to suit the situation.
 * This filtering step will frequently be applied by creating a decorated EventDetector.
 *
 * @param <T> The type of Event being detected
 */
@FunctionalInterface
public interface EventDetector<T> {

    Collection<T> findEvents(TrackPair pair);
}
