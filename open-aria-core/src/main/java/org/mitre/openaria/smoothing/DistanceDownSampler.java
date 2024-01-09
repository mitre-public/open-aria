
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.Spherical.feetPerNM;
import static org.mitre.caasd.commons.Time.theDuration;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Time;

/**
 * A DistanceDownSampler will "thin out" a Track that contains nearly-duplicate Point data because
 * the aircraft being tracked is stationary. Reducing the number/frequency of points can be
 * important for some analytics or processes that are bogged down by high point density.
 * <p>
 * (This is ARIA's replacement for StationaryPointFilter which is compute intensive)
 */
public class DistanceDownSampler implements DataCleaner<MutableTrack> {

    private final double requiredDistSeparationInNM;

    private final Duration heartBeatFreq;

    /**
     * Create a DistanceDownSampler that ensures no two sequential points (in the output track) will
     * be within 10 feet of each other unless those two points are also separated by at least 30
     * seconds or more. This constructor is equivalent to DistanceDownSampler(10.0/6076.11,
     * Duration.ofSeconds(30))
     */
    public DistanceDownSampler() {
        this(10.0 / feetPerNM(), Duration.ofSeconds(30));
    }

    /**
     * Create a DistanceDownSampler that removes "subsequent" points if they are too close to an
     * earlier point in the track (unless there is a large time separation between the subsequent
     * point and the earlier reference point).
     *
     * @param distInNm      The minimum allowable distance spacing between two sequential points in
     *                      the output Tracks
     * @param heartBeatFreq Unless the subsequent track point is separated by this time delta. This
     *                      parameter helps maintain a "heartbeat" of track data when the track in
     *                      question is stationary.
     */
    public DistanceDownSampler(double distInNm, Duration heartBeatFreq) {
        this.requiredDistSeparationInNM = distInNm;
        this.heartBeatFreq = checkNotNull(heartBeatFreq);
    }

    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        Iterator<MutablePoint> iter = track.points().iterator();

        LatLong anchor = null;
        Instant anchorTime = null;

        while (iter.hasNext()) {
            Point point = iter.next();

            //the 1st time through this loop set the anchor information
            if (anchor == null) {
                anchor = point.latLong();
                anchorTime = point.time();
                continue;
            }

            if (tooCloseInSpace(anchor, point) && tooCloseInTime(anchorTime, point.time())) {
                iter.remove();
            } else {
                anchor = point.latLong();
                anchorTime = point.time();
            }
        }

        return Optional.of(track);
    }

    private boolean tooCloseInSpace(LatLong anchor, Point point) {
        return anchor.distanceInNM(point.latLong()) < requiredDistSeparationInNM;
    }

    private boolean tooCloseInTime(Instant anchorTime, Instant pointTime) {
        Duration timeDelta = Time.durationBtw(anchorTime, pointTime);
        return theDuration(timeDelta).isLessThan(heartBeatFreq);
    }
}
