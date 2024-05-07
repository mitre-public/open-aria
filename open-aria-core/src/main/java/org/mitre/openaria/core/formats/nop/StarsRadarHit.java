package org.mitre.openaria.core.formats.nop;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Double.parseDouble;
import static org.mitre.openaria.core.formats.nop.NopMessageType.STARS_RADAR_HIT;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseInteger;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.parseString;


/**
 * A StarsRadarHit represents the data found within a single "STARS Radar Hit" message from a NOP
 * data file.
 * <p>
 * If a NOP file contains STARS data a randomly selected line from the file might look like this:
 * "[RH],STARS,A80_B,07/10/2016,12:48:02.483,DAL1419,B752,D,2672,059,282,357,033.47637,-084.04471,2874,2672,37.2195,18.4657,1,O,A,A80,,ONY,ATL,1244,ATL,ACT,IFR,,01596,,,,,,L,1,,0,{RH}"
 * <p>
 * NOP files also periodically contain lines like:
 * <p>
 * "[Bytes]7884639{Bytes}" <br> "[HB],7/12/2016 19:16:32,EOS:10,{HB}"
 */
public class StarsRadarHit extends NopRadarHit {

    public StarsRadarHit(String rawTextInput) {
        super(rawTextInput);

        checkArgument(
            rawTextInput.startsWith(STARS_RADAR_HIT.messagePrefix()),
            "Stars messages always start with: " + STARS_RADAR_HIT.messagePrefix()
        );
    }

    @Override
    public NopMessageType getNopType() {
        return STARS_RADAR_HIT;
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

    public String ptdTime() {
        return parseString(token(25));
    }

    public String trackStatus() {
        return parseString(token(27));
    }

    public String systemFlightPlanNumber() {
        return parseString(token(30));
    }
}
