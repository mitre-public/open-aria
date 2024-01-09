
package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.core.IfrVfrStatus.IFR;
import static org.mitre.openaria.core.IfrVfrStatus.VFR;

import java.time.Instant;
import java.util.Collection;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Range;

/**
 * An IfrVfrAssigner determines the IFR/VFR status of Track at a given point in Time.
 * <p>
 * This determination is made by examining the meta data available in a Tracks component Points.
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
    private int numPointsToConsider;

    /**
     * Create an object that can determine the IFR/VFR status of a Track at a particular moment in
     * time.
     *
     * @param vfrRange            The range of beacon code that are considered VFR beacons
     * @param numPointsToConsider The number of points to consider when making the determination
     *                            (must be odd)
     */
    public IfrVfrAssigner(Range vfrRange, int numPointsToConsider) {
        this.vfrBeaconRange = checkNotNull(vfrRange);
        checkArgument(numPointsToConsider % 2 == 1, "numPointsToConsider must be odd: " + numPointsToConsider);
        this.numPointsToConsider = numPointsToConsider;
    }

    /**
     * Create an IfrVfrAssigner with a VFR-Range of 1200-1277 (inclusive) that examines the 7
     * nearest points with making a IFR/VFR determination of a particular point in time.
     */
    public IfrVfrAssigner() {
        this(DEFAULT_VFR_BEACON_RANGE, DEFAULT_NUM_POINTS);
    }

    public IfrVfrStatus statusOf(Track track, Instant time) {
        checkNotNull(track);
        checkNotNull(time);
        checkArgument(
            track.asTimeWindow().contains(time),
            "This track does not exist at this moment in time"
        );

        EnumMultiset<IfrVfrStatus> counts = EnumMultiset.create(IfrVfrStatus.class);
        Collection<Point> localPoints = track.kNearestPoints(time, numPointsToConsider);

        for (Point point : localPoints) {
            counts.add(statusOf(point));
        }
        return (counts.count(IFR) > counts.count(VFR)) ? IFR : VFR;
    }

    /**
     * Determine the IFR/VFR status of a Point. This method does not merely return the Flight Rules
     * field because this piece of meta data is not reliable. Instead, this method examines several
     * pieces of (more reliable) meta data (callsign and beacon code) to make the determination.
     *
     * @param point
     *
     * @return IFR or VFR.
     */
    public IfrVfrStatus statusOf(Point point) {

        if (point.hasValidBeaconActual() && beaconIsVfr(point.beaconActualAsInt())) {
            return VFR;
        }

        if (point.hasValidCallsign() && point.hasFlightRules()) {
            return point.flightRulesAsEnum();
        }
        //...at this point not 1200, and either missing callSign or FlightRules
        return (point.hasValidCallsign()) ? IFR : VFR;
    }

    private boolean beaconIsVfr(int beacon) {
        return vfrBeaconRange.contains(beacon) || beacon == 0;
    }

    public boolean isIfrVfr(TrackPair trackPair, Instant time) {
        return statusOf(trackPair.track1(), time) != statusOf(trackPair.track2(), time);
    }
}
