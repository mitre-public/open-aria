package org.mitre.openaria.core.temp;

import org.mitre.caasd.commons.parsing.nop.NopMessage;
import org.mitre.caasd.commons.parsing.nop.NopMessageType;
import org.mitre.caasd.commons.parsing.nop.NopRadarHit;
import org.mitre.openaria.core.CsvPositioner;

/**
 * Temporary code to convert NOP data to "Generic CSV data"
 */
public class NopToCsv {

    /**
     * Converts a String of NOP data to a String of "simplified CSV data".
     *
     * The PURPOSE is to strip all "format specific data" from the "general" data model.
     */
    public static String nopToGenericCsv(String nopString) {

        NopMessage nopMessage = NopMessageType.parse(nopString);
        NopRadarHit rhMessage = (NopRadarHit) nopMessage;

        String[] tokens = nopString.split(",");

        return ",," + CsvPositioner.formatTime(rhMessage.time()) + ","
            + rhMessage.latitude() + "," + rhMessage.longitude() + ","
            + rhMessage.altitudeInHundredsOfFeet() * 100 + ","
            + rhMessage.facility() + ","
            + tokens[14] + ","
            + rhMessage.callSign();
    }


    public static void main(String[] args) {




    }
}
