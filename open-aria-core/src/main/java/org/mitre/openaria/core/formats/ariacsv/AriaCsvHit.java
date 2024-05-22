package org.mitre.openaria.core.formats.ariacsv;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.mitre.caasd.commons.LatLong.checkLatitude;
import static org.mitre.caasd.commons.LatLong.checkLongitude;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.HasPosition;
import org.mitre.caasd.commons.HasTime;
import org.mitre.caasd.commons.LatLong;


/**
 * An AriaCsvHit embodies the default data format for OpenARIA. This default CSV format provides a
 * human-readable, human-writeable (or at least writeable by a simple computer program) format for
 * Aircraft position data. The goal of the format is to provide a straight-forward,
 * easily-debuggable, way to (A) archive data for unit-tests, and (B) provide a mechanism to port
 * external data into OpenARIA that does not require Java programming.
 */
public class AriaCsvHit implements HasTime, HasPosition, Comparable<AriaCsvHit> {

    private final String rawCsv;

    /*
     * This array contains the index values where commas are found in the raw text. This is a more
     * memory compact way to enable quick answers to methods like "callSign()" and "speed()".
     * Storing these values directly wastes memory and recompute rawTextInput.split(",") can
     * drastically slow down processes to manipulate points.
     *
     * Note: This array could be typed as a byte[] but it would require using unsigned bytes. This
     * is not implemented because it is too small a benefit (save about 20 bytes per AriaCsvHit)
     * for the added complexity.
     */
    private final short[] commaIndices;

    /*
     * Time is stored directly because hits are often sorted by time. Ensuring this value
     * is only parsed once significantly improves performance.
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

    /**
     * A unique ID number that identifies a specific vehicle within a large dataset of many location
     * measurements.
     */
    private final String linkId;

    /**
     * Eagerly parse: {time, latitude, longitude, and linkId} from this CSV.  Other fields are
     * extract lazily
     *
     * @param rawCsv A String of Comma Separated Values that matches the OpenARIA format.
     */
    public AriaCsvHit(String rawCsv) {

        requireNonNull(rawCsv);
        this.rawCsv = rawCsv;

        this.commaIndices = findTokenIndices(rawCsv);

        //these fields are cached for speed
        this.time = Instant.parse(token(2));
        this.linkId = token(3);
        this.latitude = Double.parseDouble(token(4));
        this.longitude = Double.parseDouble(token(5));
        checkLatitude(latitude);
        checkLongitude(longitude);
    }

    private static short[] findTokenIndices(String rawCsv) {

        char[] text = rawCsv.toCharArray();

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

    /** @return The i_th token in the CSV row. */
    public String token(int index) {

        if (index > commaIndices.length) {
            throw new ArrayIndexOutOfBoundsException("Requested token " + index + " but there are only " + commaIndices.length + " commas");
        }

        if (index == 0) {
            return rawCsv.substring(0, commaIndices[index]).intern();
        }

        //if there is text after the last comma we still want to return that text
        if (index == commaIndices.length) {
            return rawCsv.substring(commaIndices[index - 1] + 1, rawCsv.length()).intern();
        }

        return rawCsv.substring(commaIndices[index - 1] + 1, commaIndices[index]).intern();

    }

    public static AriaCsvHit from(String csvText) {
        return new AriaCsvHit(csvText);
    }


    /*
     * @return A String that identifies a particular entity in a stream of position data (e.g., a
     *     specific aircraft, vehicle, or person). This id is used to link multiple Points
     *     describing the same entity together across time (e.g. form movement paths). It is also
     *     used to distinguish distinct entities within a datafeed.
     */
    public String linkId() {
        return this.linkId;
    }

    @Override
    public Instant time() {
        return time;
    }

    @Override
    public LatLong latLong() {
        return LatLong.of(latitude, longitude);
    }

    public Distance altitude() {
        Double altInFeet = parseDouble(token(6));
        return nonNull(altInFeet) ? Distance.ofFeet(altInFeet) : null;
    }

    public String rawCsvText() {
        return this.rawCsv;
    }

    public static Double parseDouble(String x) {
        try {
            return Double.valueOf(x);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public int compareTo(AriaCsvHit o) {
        int timeResult = time().compareTo(o.time());
        if (timeResult != 0) {
            return timeResult;
        }

        return rawCsv.compareTo(o.rawCsv);
    }
}