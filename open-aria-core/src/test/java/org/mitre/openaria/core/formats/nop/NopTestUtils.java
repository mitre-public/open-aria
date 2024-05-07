package org.mitre.openaria.core.formats.nop;


public class NopTestUtils {

    private static void triggerCommonLazyParsing(NopRadarHit nopPoint) {

        /*
         * For each method declared in NopRadarHit: confirm these lazily parsed fields are (1)
         * actually parsed and (2) no Exceptions like NumberFormatExceptions (or similar) are
         * thrown. Note: This test DOES NOT confirm that the parsing extracts the correct value.
         *
         * This method is designed to support BULK parsing test to find any "one in a million"
         * formating errors.
         */
        nopPoint.facility();
        nopPoint.time();
        nopPoint.callSign();
        nopPoint.aircraftType();
        nopPoint.equipmentTypeSuffix();
        nopPoint.reportedBeaconCode();
        nopPoint.altitudeInHundredsOfFeet();
        nopPoint.speed();
        nopPoint.heading();
        nopPoint.latitude();
        nopPoint.longitude();
        nopPoint.sensorIdLetters();
        nopPoint.arrivalAirport();
        nopPoint.flightRules();
        nopPoint.heavyLargeOrSmall();
        nopPoint.onActiveSensor();
    }

    static void triggerLazyParsing(AgwRadarHit agwPoint) {

        triggerCommonLazyParsing(agwPoint);

        /*
         * For each method declared in AgwRadarHit: confirm these lazily parsed fields are (1)
         * actually parsed and (2) no Exceptions like NumberFormatExceptions (or similar) are
         * thrown. Note: This test DOES NOT confirm that the parsing extracts the correct value.
         *
         * This method is designed to support BULK parsing test to find any "one in a million"
         * formating errors.
         */
        agwPoint.trackNumber();
        agwPoint.assignedBeaconCode();
        agwPoint.x();
        agwPoint.y();
        agwPoint.keyboard();
        agwPoint.positionSymbol();
        agwPoint.arrivalDepartureStatus();
        agwPoint.scratchpad1();
        agwPoint.entryFix();
        agwPoint.exitFix();
        agwPoint.fdfNumber();
        agwPoint.sequenceNumber();
        agwPoint.departureAirport();
        agwPoint.sensor();
        agwPoint.scratchpad2();
        agwPoint.additionalFacilityAlphaChar();
    }

    static void triggerLazyParsing(CenterRadarHit centerPoint) {

        triggerCommonLazyParsing(centerPoint);

        /*
         * For each method declared in CenterRadarHit: confirm these lazily parsed fields are (1)
         * actually parsed and (2) no Exceptions like NumberFormatExceptions (or similar) are
         * thrown. Note: This test DOES NOT confirm that the parsing extracts the correct value.
         *
         * This method is designed to support BULK parsing test to find any "one in a million"
         * formating errors.
         */
        centerPoint.computerId();
        centerPoint.controllingFacilitySector();
        centerPoint.coordinationTime();
        centerPoint.cmsField153A();
        centerPoint.sequenceNumber();
        centerPoint.departureAirport();
        centerPoint.eta();
        centerPoint.reportInterimAssignAltitude();
    }

    static void triggerLazyParsing(StarsRadarHit starsPoint) {

        triggerCommonLazyParsing(starsPoint);

        /*
         * For each method declared in StarsRadarHit: confirm these lazily parsed fields are (1)
         * actually parsed and (2) no Exceptions like NumberFormatExceptions (or similar) are
         * thrown. Note: This test DOES NOT confirm that the parsing extracts the correct value.
         *
         * This method is designed to support BULK parsing test to find any "one in a million"
         * formating errors.
         */
        starsPoint.trackNumber();
        starsPoint.assignedBeaconCode();
        starsPoint.x();
        starsPoint.y();
        starsPoint.keyboard();
        starsPoint.positionSymbol();
        starsPoint.arrivalDepartureStatus();
        starsPoint.scratchpad1();
        starsPoint.entryFix();
        starsPoint.exitFix();
        starsPoint.ptdTime();
        starsPoint.trackStatus();
        starsPoint.systemFlightPlanNumber();

    }

    static void triggerLazyParsing(MeartsRadarHit meartsPoint) {

        triggerCommonLazyParsing(meartsPoint);

        /*
         * For each method declared in MeartsRadarHit: confirm these lazily parsed fields are (1)
         * actually parsed and (2) no Exceptions like NumberFormatExceptions (or similar) are
         * thrown. Note: This test DOES NOT confirm that the parsing extracts the correct value.
         *
         * This method is designed to support BULK parsing test to find any "one in a million"
         * formatting errors.
         */

        meartsPoint.x();
        meartsPoint.y();
        meartsPoint.controllingFacilitySector();
        meartsPoint.departureAirport();
        meartsPoint.eta();
        meartsPoint.reportInterimAssignAltitude();

    }

}
