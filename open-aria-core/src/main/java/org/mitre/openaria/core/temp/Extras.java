package org.mitre.openaria.core.temp;

import static java.util.Objects.isNull;

/**
 * This is a temporary class to keeping scaffolding needed to slowly "pull apart" the Point
 * interface so that it isn't so NOP specific.
 */
public class Extras {

    /** A Mixin (i.e., Trait) interface for declaring that a TrackId is available. */
    @FunctionalInterface
    public interface HasTrackId {
        String trackId();
    }

    @FunctionalInterface
    public interface HasSourceDetails {
        SourceDetails sourceDetails();
    }

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

    /*
     * These are all the fields in Point that need to be migrated OUT of the original Point interface
     * and into a data format
     */
//    public String beaconActual();
//
//    public String beaconAssigned();
//
//    public Double course();
//
//    public Double speedInKnots();
}
