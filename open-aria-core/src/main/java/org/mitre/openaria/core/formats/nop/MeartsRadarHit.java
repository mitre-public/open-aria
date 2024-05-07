package org.mitre.openaria.core.formats.nop;

import static java.lang.Double.parseDouble;
import static org.mitre.openaria.core.formats.nop.NopMessageType.MEARTS_RADAR_HIT;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseString;

import com.google.common.base.Preconditions;


/**
 * A MEARTSRadarHit represents the data found within a single "MEARTS Radar Hit" message from a NOP
 * data file.
 * <p>
 * If a NOP file contains MEARTS data a randomly selected line from the file might look like this:
 * "[RH],MEARTS,ZUA_B,11-05-2019,15:26:41.020,,,,,0,0,0,013.5356,144.9143,,,258.0105,258.0056,,ZUA/1F,,ZUA_B,,,,,,,,,,,,,0//,,,1,{RH}"
 * <p>
 * NOP files also periodically contain lines like:
 * <p>
 * "[Bytes]2267027{Bytes}" <br> "[HB],11/5/2019 15:26:56,EOF:1000,{HB}"
 */
public class MeartsRadarHit extends NopRadarHit {

    public MeartsRadarHit(String rawTextInput) {
        super(rawTextInput);

        Preconditions.checkArgument(
            rawTextInput.startsWith(MEARTS_RADAR_HIT.messagePrefix()),
            "MEARTS messages always start with: " + MEARTS_RADAR_HIT.messagePrefix()
        );
    }

    @Override
    public NopMessageType getNopType() {
        return MEARTS_RADAR_HIT;
    }

    public String computerId() {
        return parseString(token(14));
    }

    public Double x() {
        return parseDouble(token(16));
    }

    public Double y() {
        return parseDouble(token(17));
    }

    public String controllingFacilitySector() {
        return parseString(token(19));
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
