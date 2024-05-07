package org.mitre.openaria.core.formats.nop;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.caasd.commons.LatLong.checkLatitude;
import static org.mitre.caasd.commons.LatLong.checkLongitude;
import static org.mitre.openaria.core.formats.nop.NopParsingUtils.*;

import java.time.Instant;


/**
 * A NopRadarHit represents the pieces of data that are common to all four NOP Radar hit types: AGW,
 * CENTER, STARS, and MEARTS. Consequently, a NopRadarHit contains methods to retrieve information
 * like "latitude", "longitude", "time", and "callSign", etc...
 * <p>
 * NopRadarHit is package-private because it does not correspond to a real NopMessageType.
 */
public abstract class NopRadarHit implements NopMessage{

    private final String rawTextInput;

    /*
     * This array contains the index values where commas are found in the raw text. This is a more
     * memory compact way to enable quick answers to methods like "callSign()" and "speed()".
     * Storing these values directly wastes memory and recompute rawTextInput.split(",") can
     * drastically slow down processes to manipulate points.
     *
     * Note: This array could be typed as a byte[] but it would require using unsigned bytes. This
     * is not implemented because it is too small a benefit (save about 20 bytes per NopRadarHit)
     * for the added complexity.
     */
    private final short[] commaIndices;

    /*
     * Time is stored directly because NOP data is often sorted by time. Thus, ensuring this value
     * is only parsed once can significantly improve performance in some cases.
     */
    private final Instant time;

    /*
     * Latitude is stored directly because NOP data is often sorted by location. Thus, ensuring this
     * value is only parsed once can significantly improve performance in some cases. This value is
     * stored as a primitive to save memory.
     */
    private final double latitude;

    /*
     * Longitude is stored directly because NOP data is often sorted by location. Thus, ensuring
     * this value is only parsed once can significantly improve performance in some cases. This value
     * is stored as a primitive to save memory.
     */
    private final double longitude;

    public NopRadarHit(String rawTextInput) {

        checkArgument(rawTextInput.startsWith("[RH],"));

        this.rawTextInput = rawTextInput;

        this.commaIndices = findTokenIndices();

        //these fields are cached for speed
        this.time = parseNopTime(token(3), token(4));
        this.latitude = Double.parseDouble(token(12));
        this.longitude = Double.parseDouble(token(13));
        checkLatitude(latitude);
        checkLongitude(longitude);
    }

    private short[] findTokenIndices() {

        char[] text = rawTextInput.toCharArray();

        //how big is the output array?
        int count = 0;
        for (int i = 0; i < text.length; i++) {
            if (text[i] == ',') {
                count++;
            }
        }

        //make the output array
        short[] output = new short[count];
        short counter = 0;
        for (int i = 0; i < text.length; i++) {

            if (text[i] == ',') {
                output[counter] = (short) i;
                counter++;
            }
        }

        return output;
    }

    protected String token(int index) {

        if (index == 0) {
            return rawTextInput.substring(0, commaIndices[index]).intern();
        } else {
            return rawTextInput.substring(commaIndices[index - 1] + 1, commaIndices[index]).intern();
        }
    }

    @Override
    public String rawMessage() {
        return this.rawTextInput;
    }

    public String facility() {
        return parseFacility(token(2)).intern();
    }

    public Instant time() {
        return time;
    }

    public String callSign() {
        return parseString(token(5));
    }

    public String aircraftType() {
        return parseString(token(6));
    }

    public String equipmentTypeSuffix() {
        return parseString(token(7));
    }

    public String reportedBeaconCode() {
        return parseString(token(8));
    }

    public Integer altitudeInHundredsOfFeet() {
        return parseInteger(token(9));
    }

    public Double speed() {
        return NopParsingUtils.parseDouble(token(10));
    }

    public Double heading() {
        return NopParsingUtils.parseDouble(token(11));
    }

    public Double latitude() {
        return latitude;
    }

    public Double longitude() {
        return longitude;
    }

    public String sensorIdLetters() {
        return parseString(token(21));
    }

    public String arrivalAirport() {
        return parseString(token(26));
    }

    public String flightRules() {
        return parseString(token(28));
    }

    public String heavyLargeOrSmall() {
        return parseString(token(36));
    }

    public Boolean onActiveSensor() {
        return parseBoolean(token(37));
    }
}
