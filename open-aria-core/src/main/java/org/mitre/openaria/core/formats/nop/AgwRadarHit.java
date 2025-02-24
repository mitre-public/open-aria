package org.mitre.openaria.core.formats.nop;

import static java.lang.Double.parseDouble;
import static org.mitre.openaria.core.formats.nop.NopMessageType.AGW_RADAR_HIT;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseInteger;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseString;

import com.google.common.base.Preconditions;


/**
 * A AgwRadarHit represents the data found within a single "AGW Radar Hit" message from a NOP data
 * file.
 * <p>
 * If a NOP file contains AGW data a randomly selected line from the file might look like this:
 * "[RH],AGW,ABI_B,07/12/2016,19:21:08.848,N832AT,PA44,,5136,101,144,251,032.62683,-099.43983,088,5136,9.69,15.09,1,B,0,ABI,MAF,MWL,BGS,,MAF,,IFR,,39,39,TKI,,00,,S,0,,0,,94.59,96.59,{RH}"
 * <p>
 * NOP files also periodically contain lines like:
 * <p>
 * "[Bytes]7884639{Bytes}" <br> "[HB],7/12/2016 19:16:32,EOS:10,{HB}"
 */
@SuppressWarnings("GrazieInspection")
public class AgwRadarHit extends NopRadarHit {

    public AgwRadarHit(String rawTextInput) {
        super(rawTextInput);

        Preconditions.checkArgument(
            rawTextInput.startsWith(AGW_RADAR_HIT.messagePrefix()),
            "Agw messages always start with: " + AGW_RADAR_HIT.messagePrefix()
        );
    }

    @Override
    public NopMessageType getNopType() {
        return AGW_RADAR_HIT;
    }

    public String trackNumber() {
        return token(14);
    }

    public Integer assignedBeaconCode() {
        return parseInteger(token(15));
    }

    public Double x() {
        return parseDouble(token(16));
    }

    public Double y() {
        return parseDouble(token(17));
    }

    public String keyboard() {
        return parseString(token(18));
    }

    public String positionSymbol() {
        return parseString(token(19));
    }

    public String arrivalDepartureStatus() {
        return parseString(token(20));
    }

    public String scratchpad1() {
        return parseString(token(22));
    }

    public String entryFix() {
        return parseString(token(23));
    }

    public String exitFix() {
        return parseString(token(24));
    }

    public String fdfNumber() {
        return parseString(token(30));
    }

    public String sequenceNumber() {
        return parseString(token(31));
    }

    public String departureAirport() {
        return parseString(token(32));
    }

    public String sensor() {
        return parseString(token(34));
    }

    public String scratchpad2() {
        return parseString(token(35));
    }

    public String additionalFacilityAlphaChar() {
        return parseString(token(38));
    }
}
