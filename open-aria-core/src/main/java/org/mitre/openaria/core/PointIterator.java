
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.mitre.openaria.core.formats.NopHit;
import org.mitre.openaria.core.formats.nop.NopMessage;
import org.mitre.openaria.core.formats.nop.NopParser;
import org.mitre.openaria.core.formats.nop.NopRadarHit;


/**
 * A PointIterator wraps a NopParser or a SWIMParser. The Iterator ensure
 * <p>
 */
public class PointIterator implements Iterator<Point<NopHit>> {

    private final Iterator<NopMessage> nopMessageIter;

    Point<NopHit> nextPoint;

    public PointIterator(NopParser parser) {
        this.nopMessageIter = checkNotNull(parser, "The NopParser cannot be null");
        this.nextPoint = getNext();
    }

    @Override
    public boolean hasNext() {
        return nextPoint != null;
    }

    @Override
    public Point<NopHit> next() {
        Point<NopHit> returnMe = nextPoint;
        this.nextPoint = getNext();
        return returnMe;
    }

    private Point<NopHit> getNext() {

        if (usingNop()) {
            return getNextPointFromNop();
        } else {
            throw new AssertionError("Unexpected state");
        }
    }

    private boolean usingNop() {
        return this.nopMessageIter != null;
    }

    private Point<NopHit> getNextPointFromNop() {

        while (nopMessageIter.hasNext()) {
            NopMessage message = nopMessageIter.next();

            if (message.getNopType().isRadarHit()) {
                return NopHit.from((NopRadarHit) message);
            } else {
                //ignore items like FlightPlan messages and conflict alerts
            }
        }
        return null;
    }

}
