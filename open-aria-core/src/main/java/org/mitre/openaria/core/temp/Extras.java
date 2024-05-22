package org.mitre.openaria.core.temp;

import static java.util.Objects.isNull;

/**
 * This is a temporary class to keeping scaffolding needed to slowly "pull apart" the Point
 * interface so that it isn't so NOP specific.
 */
public class Extras {

    /** A Mixin (i.e., Trait) interface for declaring that Sensor and Facility are available. */
    @FunctionalInterface
    public interface HasSourceDetails {
        SourceDetails sourceDetails();
    }

    /** A Mixin (i.e., Trait) interface for declaring that callsign and aircraftType are available. */
    @FunctionalInterface
    public interface HasAircraftDetails {
        AircraftDetails acDetails();

        default String callsign() {
            return acDetails().callsign;
        }

        default String aircraftType() {
            return acDetails().aircraftType;
        }

        static String castAndGetCallsign(Object o) {
            if (o instanceof HasAircraftDetails had) {
                return had.callsign();
            } else {
                throw new IllegalArgumentException("Must be instanceof HasAircraftDetails");
            }
        }
    }

    /** A Mixin (i.e., Trait) interface for declaring that IFR/VFR is available. */
    @FunctionalInterface
    public interface HasFlightRules {
        String flightRules();
    }

    @FunctionalInterface
    public interface HasBeaconCodes {
        BeaconCodes beaconCodes();

        default String beaconActual() {
            BeaconCodes bc = beaconCodes();
            return isNull(beaconCodes()) ? null : bc.actual;
        }

        default String beaconAssigned() {
            BeaconCodes bc = beaconCodes();
            return isNull(beaconCodes()) ? null : beaconCodes().assigned;
        }
    }


    public record SourceDetails(String sensor, String facility) {
    }


    public record AircraftDetails(String callsign, String aircraftType) {

    }

    public record BeaconCodes(String actual, String assigned) {

    }
}
