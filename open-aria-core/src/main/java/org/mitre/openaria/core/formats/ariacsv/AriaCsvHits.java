package org.mitre.openaria.core.formats.ariacsv;

import org.mitre.caasd.commons.Position;
import org.mitre.openaria.core.Point;

public class AriaCsvHits {

    /**
     * Convert a line of "OpenARIA CSV text" to a {@code Point<AriaCsvHit>}
     *
     * @param rawCsvText a line of CSV
     *
     * @return A Point record backed by an AriaCsvHit
     */
    public static Point<AriaCsvHit> parsePointFromAriaCsv(String rawCsvText) {

        AriaCsvHit ariaHit = AriaCsvHit.from(rawCsvText);

        Position pos = new Position(ariaHit.time(), ariaHit.latLong(), ariaHit.altitude());

        return new Point<>(pos, null, ariaHit.linkId(), ariaHit);
    }
}
