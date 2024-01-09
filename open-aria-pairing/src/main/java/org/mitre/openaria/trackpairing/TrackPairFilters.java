
package org.mitre.openaria.trackpairing;

import java.time.Duration;
import java.util.function.Consumer;

import org.mitre.openaria.core.TrackPair;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.DataFilter;
import org.mitre.caasd.commons.Distance;

/**
 * This class provides public access, via factory methods, to DataCleaners and DataFilters.
 */
public class TrackPairFilters {

    public static DataFilter<TrackPair> requireSeparation(double reqSeparationInNauticalMiles) {
        return new RequireSeparation(reqSeparationInNauticalMiles);
    }

    public static DataFilter<TrackPair> requireSeparation(double reqSeparationInNauticalMiles, Consumer<TrackPair> onRemoval) {
        return new RequireSeparation(reqSeparationInNauticalMiles, onRemoval);
    }

    public static DataFilter<TrackPair> tracksMustOverlapInTime(Duration requiredOverlap) {
        return new TracksMustOverlapInTime(requiredOverlap);
    }

    public static DataFilter<TrackPair> tracksMustOverlapInTime(Consumer<TrackPair> onRemoval, Duration requiredOverlap) {
        return new TracksMustOverlapInTime(onRemoval, requiredOverlap);
    }

    public static IsFormationFlight formationFilter(Duration requiredTimeInFormation, Distance distInNm) {
        return new IsFormationFlight(requiredTimeInFormation, distInNm);
    }

    public static DataCleaner<TrackPair> removeFormationFlights(IsFormationFlight predicate) {
        return new RemoveFormationFlights(predicate);
    }

    public static DataCleaner<TrackPair> removeFormationFlights(IsFormationFlight predicate, Consumer<TrackPair> onRemoval) {
        return new RemoveFormationFlights(predicate, onRemoval);
    }

}
