package org.mitre.openaria.core.formats;

import java.io.File;
import java.util.Base64;
import java.util.Iterator;

import org.mitre.openaria.core.Point;

/**
 * A format allows OpenARIA to convert an arbitrary File of input location data into a sequence of
 * Points. It also allows OpenARIA to copy raw input back into the output records (for event
 * traceability).
 *
 * @param <T> NopHit, AriaCsvHit, SwimTaisHit
 */
public interface Format<T> {

    /** Convert a file of location data written in some unknown format into an Iterator of Points. */
    Iterator<Point<T>> parseFile(File file);

    /**
     * Given a positionReport (of format T) provide a json-friendly String that describes the
     * positionReport. When no JSON-friendly String exists consider converting the positionReport to
     * a byte[] and using the "Format.asBase64(byte[])" helper method.
     * <p>
     * Required to copy input location data into output event records. This allows output event
     * records to say: "This input data caused this output event"
     */
    String asRawString(T positionReport);


    /** Uses java.util.Base64's unpadded url encoder to produce a json-friendly String. */
    static String asBase64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Uses java.util.Base64's unpadded url dencoder to interpret a json-friendly String. */
    static byte[] fromBase64(String base64Encoding) {
        return Base64.getUrlDecoder().decode(base64Encoding);
    }
}
