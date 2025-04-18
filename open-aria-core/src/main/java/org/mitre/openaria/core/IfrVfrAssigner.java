
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.core.IfrVfrStatus.IFR;
import static org.mitre.openaria.core.IfrVfrStatus.VFR;

import java.time.Instant;
import java.util.Collection;

import org.mitre.openaria.core.formats.nop.NopEncoder;
import org.mitre.openaria.core.formats.nop.NopHit;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Range;

/**
 * An IfrVfrAssigner determines the IFR/VFR status of Track at a given point in Time.
 * <p>
 * This determination is made by examining the metadata available in a Tracks component Points.
 */
public class IfrVfrAssigner {

    private static final Range<Integer> DEFAULT_VFR_BEACON_RANGE = Range.closed(1200, 1277);

    private static final int DEFAULT_NUM_POINTS = 7;

    /** A Beacon code in this range is considered a VFR beacon code. */
    private final Range<Integer> vfrBeaconRange;

    /**
     * Examine this many points to make a determination about if a flight is VFR or IFR at a
     * particular point in time.
     */
    private final int numPointsToConsider;

    /** Converts Points into NopPoints so that we can access the toString(). */
    private final NopEncoder nopEncoder = new NopEncoder();

    /**
     * Create an object that can determine the IFR/VFR status of a Track at a particular moment in
     * time.
     *
     * @param vfrRange            The range of beacon code that are considered VFR beacons
     * @param numPointsToConsider The number of points to consider when making the determination
     *                            (must be odd)
     */
    public IfrVfrAssigner(Range<Integer> vfrRange, int numPointsToConsider) {
        this.vfrBeaconRange = checkNotNull(vfrRange);
        checkArgument(numPointsToConsider % 2 == 1, "numPointsToConsider must be odd: " + numPointsToConsider);
        this.numPointsToConsider = numPointsToConsider;
    }

    /**
     * Create an IfrVfrAssigner with a VFR-Range of 1200-1277 (inclusive) that examines the 7
     * nearest points with making an IFR/VFR determination of a particular point in time.
     */
    public IfrVfrAssigner() {
        this(DEFAULT_VFR_BEACON_RANGE, DEFAULT_NUM_POINTS);
    }

    public <T> IfrVfrStatus statusOf(Track<T> track, Instant time) {
        checkNotNull(track);
        checkNotNull(time);
        checkArgument(
            track.asTimeWindow().contains(time),
            "This track does not exist at this moment in time"
        );

        EnumMultiset<IfrVfrStatus> counts = EnumMultiset.create(IfrVfrStatus.class);
        Collection<Point<T>> localPoints = track.kNearestPoints(time, numPointsToConsider);

        for (Point<T> point : localPoints) {
            counts.add(statusOf(point));
        }
        return (counts.count(IFR) > counts.count(VFR)) ? IFR : VFR;
    }

    /**
     * Determine the IFR/VFR status of a Point. This method does not merely return the Flight Rules
     * field because this piece of metadata is not reliable. Instead, this method examines several
     * pieces of (more reliable) metadata (callsign and beacon code) to make the determination.
     *
     * @param point
     *
     * @return IFR or VFR.
     */
    public IfrVfrStatus statusOf(Point<?> point) {

        Point<NopHit> np = NopHit.from(nopEncoder.asRawNop(point));

        if (np.rawData().hasValidBeaconActual() && beaconIsVfr(np.rawData().beaconActualAsInt())) {
            return VFR;
        }

        if (np.hasValidCallsign() && np.rawData().hasFlightRules()) {
            return IfrVfrStatus.from(np.rawData().flightRules());
        }
        //...at this point not 1200, and either missing callSign or FlightRules
        return (np.hasValidCallsign()) ? IFR : VFR;
    }

    private boolean beaconIsVfr(int beacon) {
        return vfrBeaconRange.contains(beacon) || beacon == 0;
    }
}
