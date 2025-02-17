package org.mitre.openaria.core.formats;

import java.io.File;
import java.util.Iterator;

import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvHit;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvParser;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopParser;

public class Formats {

    /**
     * @param format One of {"NOP" or "CSV"} (case-insensitive).  (Supporting FAA SWIM data is
     *               coming soon)
     *
     * @return A Format that can help us parse a specific type of data
     */
    public static Format<?> getFormat(String format) {

        String cleanFormat = format.trim().toLowerCase();

        if (cleanFormat.equals("nop")) {
            return Formats.nop();
        }

        if (cleanFormat.equals("csv")) {
            return Formats.csv();
        }

        throw new IllegalArgumentException("Unsupported format: " + format);
    }


    /** @return a Format that support processing NOP data. */
    public static Format<NopHit> nop() {

        return new Format<>() {
            @Override
            public String asRawString(NopHit message) {
                return message.rawMessage().rawMessage();
            }

            @Override
            public Iterator<Point<NopHit>> parseFile(File file) {
                return new PointIterator(new NopParser(file));
            }
        };
    }

    /**
     * @return a Format that support processing OpenARIA CSV data. This (overly?) plain data format
     *     exists to enable interoperability with other programs, languages, and tools.
     */
    public static Format<AriaCsvHit> csv() {

        return new Format<>() {
            @Override
            public String asRawString(AriaCsvHit message) {
                return message.rawCsvText();
            }

            @Override
            public Iterator<Point<AriaCsvHit>> parseFile(File file) {
                return new AriaCsvParser(file);
            }
        };
    }
}
