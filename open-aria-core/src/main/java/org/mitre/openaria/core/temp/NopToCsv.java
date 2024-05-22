package org.mitre.openaria.core.temp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.formats.NopHit;

/**
 * This is a "temporary class that we'll use to convert NOP data to CsvPoint data.
 */
public class NopToCsv {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

    static String toAriaCsvFormat(Point<NopHit> nop) {

        String linkId = nop.rawData().rawMessage().facility() + "-" + nop.trackId();
        String lat = String.format("%.4f", nop.latitude());
        String lng = String.format("%.4f", nop.longitude());
        String alt = String.format("%.0f", nop.altitude().inFeet());

        String rawSourceNop = nop.rawData().rawMessage().rawMessage();
        String nopAsBase64 = BASE64_ENCODER.encodeToString(rawSourceNop.getBytes());

        return ",," + nop.time().toString() + "," + linkId + ","+lat + "," + lng + "," + alt + "," + nopAsBase64;
    }


    /**
     * Converts a String of NOP data to a String of "simplified CSV data".
     *
     * The PURPOSE is to strip all "format specific data" from the "general" data model.
     */
    public static String nopToAriaCsvFormat(String nopString) {

        Point<NopHit> asPoint = NopHit.from(nopString);

        return toAriaCsvFormat(asPoint);
    }


    /** Reads a file of NOP data, converts each line to "AriaCsv" format. */
    public static void convertFileOfNop(File f) throws IOException {

        //read a File, convert each Line to a Point and collect the results in a list
        List<Point<NopHit>> points = Files.lines(f.toPath())
            .map((String s) -> NopHit.from(s))
            .collect(Collectors.toList());

        points.forEach(pt -> System.out.println(toAriaCsvFormat(pt)));
    }

    public static void main(String[] args) throws IOException {

        File nopData = new File("open-aria-airborne/src/main/resources/scaryTrackData.txt");

        convertFileOfNop(nopData);
    }

}
