package org.mitre.openaria.core.formats.nop;

import static org.mitre.openaria.core.formats.nop.NopMessageType.CENTER_RADAR_HIT;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseString;

import com.google.common.base.Preconditions;

/**
 * A CenterRadarHit represents the data found within a single "CENTER Radar Hit" message from a NOP
 * data file.
 * <p>
 * If a NOP file contains CENTER data a randomly selected line from the file might look like this:
 * "[RH],Center,ZLA_B,07-10-2016,06:35:40.000,ROU1884,A319,Z,7211,312,510,051,33.9483,-115.6908,790,,,,,ZLA/19,,ZLA_B,,,,E0625,CYYZ,,IFR,,790,1396403226,SAN,1025,312//350,,L,1,,,{RH}"
 * <p>
 * NOP files also periodically contain lines like:
 * <p>
 * "[Bytes]7884639{Bytes}" <br> "[HB],7/12/2016 19:16:32,EOS:10,{HB}"
 */
public class CenterRadarHit extends NopRadarHit {

    public CenterRadarHit(String rawTextInput) {
        super(rawTextInput);

        Preconditions.checkArgument(
            rawTextInput.startsWith(CENTER_RADAR_HIT.messagePrefix()),
            "Center messages always start with: " + CENTER_RADAR_HIT.messagePrefix()
        );

    }

    @Override
    public NopMessageType getNopType() {
        return CENTER_RADAR_HIT;
    }

    public String computerId() {
        return parseString(token(14));
    }

    public String controllingFacilitySector() {
        return parseString(token(19));
    }

    public String coordinationTime() {
        return parseString(token(25));
    }

    public String cmsField153A() {
        return parseString(token(29));
    }

    public String sequenceNumber() {
        return parseString(token(31));
    }

    public String departureAirport() {
        return parseString(token(32));
    }

    public String eta() {
        return parseString(token(33));
    }

    public String reportInterimAssignAltitude() {
        return parseString(token(34));
    }
}
