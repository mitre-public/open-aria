package org.mitre.openaria.core.temp;

/**
 * This is a temporary class to keeping scaffolding needed to slowly "pull apart" the Point
 * interface so that it isn't so NOP specific.
 */
public class Extras {

    @FunctionalInterface
    public interface HasSourceDetails {
        SourceDetails sourceDetails();
    }


    public record SourceDetails(String sensor, String facility) {
    }


    public record AircraftDetails(String callsign, String aircraftType) {

    }

    /*
     * These are all the fields in Point that need to be migrated OUT of the original Point interface
     * and into a data format
     */

//    public String callsign();
//
//    public String aircraftType();

//
//    public String sensor();
//
//    public String facility();

//
//    public String beaconActual();
//
//    public String beaconAssigned();
//
//    public String flightRules();
//
//    public Double course();
//
//    public Double speedInKnots();
//
//    public Double curvature();
}
