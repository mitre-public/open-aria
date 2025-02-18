package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Double.max;
import static org.mitre.openaria.core.Interpolate.interpolate;
import static org.mitre.openaria.core.TrackPairs.overlapInTime;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;

import org.mitre.caasd.commons.Time;
import org.mitre.caasd.commons.TimeWindow;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopParser;

/**
 * A Collection of convenience methods that operate on Tracks
 */
public class Tracks {

    /**
     * @return The most common callsign for the points inside this Track.
     */
    public static <T> String aircraftId(Track<T> track) {
        return track.callsign();
    }

    /**
     * @param track A Track
     *
     * @return True when track.aircraftId() returns null or "";
     */
    public static <T>  boolean missingAircraftId(Track<T>  track) {
        String acid = aircraftId(track);
        return acid == null || acid.isEmpty();
    }

    /**
     * @param track A Track
     *
     * @return False when track.aircraftId() returns null or "";
     */
    public static <T> boolean hasAircraftId(Track<T>  track) {
        return !missingAircraftId(track);
    }

    /**
     * Estimate the maximum lateral distance between two tracks at any given moment in time. This
     * computation is only based on lat/long data and time data. In other words, altitude data is
     * ignored. This estimate is computed by creating a synthetic/interpolated "matching point" in
     * track 1 for each Point in track 2 (and vice versa). For example, if a track 1 has a point at
     * time t then a "matching point" on track 2 is generated. This matching point also occurs at
     * time t. The lateral distance between these two points (with perfectly matching time values)
     * is then computed. The result of this method is the maximum value of these distance
     * comparisons.
     *
     * @param t1 The first Track
     * @param t2 The second Track
     *
     * @return The maximum instantaneous distance between each track (ignores altitude, only uses
     *     Lat/Long position data for this computation)
     */
    public static <T> double maxDistBetween(Track<T>  t1, Track<T> t2) {
        checkNotNull(t1, "The 1st input track is null");
        checkNotNull(t2, "The 2nd input track is null");
        checkArgument(overlapInTime(t1, t2), "The input tracks do not overlap in time");

        NavigableSet<Point<T> > t1Points = t1.points();
        NavigableSet<Point<T> > t2Points = t2.points();

        return max(
            maxDistance(t1Points, t2Points),
            maxDistance(t2Points, t1Points)
        );
    }

    /**
     * @param src         Use the time values from points in this data set to...
     * @param destination create interpolated points within this data set.
     *
     * @return The maximum distance between a point in the src dataset to its "matching point"
     *     (created using interpolation) in the destination dataset.
     */
    private static <T> double maxDistance(NavigableSet<Point<T>> src, NavigableSet<Point<T>> destination) {

        double maxDist = 0;

        for (Point<T> srcPoint : src) {

            Point<T> ceiling = destination.ceiling(srcPoint);
            Point<T> floor = destination.floor(srcPoint);

            if (ceiling == null || floor == null) {
                continue;
            }

            Point<T> interpolatedPoint = interpolate(floor, ceiling, srcPoint.time());

            double curDist = srcPoint.distanceInNmTo(interpolatedPoint);

            maxDist = max(maxDist, curDist);
        }

        return maxDist;
    }

    /**
     * Compute and return the smallest possible TimeWindow that contains all input tracks.
     *
     * @param tracks A collection of Tracks
     *
     * @return The smallest possible TimeWindow that contains all input tracks
     */
    public static <T> TimeWindow windowContaining(Collection<Track<T>> tracks) {
        checkNotNull(tracks, "The input Collection of Tracks cannot be null");
        checkArgument(!tracks.isEmpty(), "Cannot compute a TimeWindow for an empty collection of Tracks");

        Track<T> firstTrack = tracks.iterator().next();
        TimeWindow firstTimeWindow = firstTrack.asTimeWindow();

        Instant minTime = firstTimeWindow.start();
        Instant maxTime = firstTimeWindow.end();

        for (Track<T> track : tracks) {

            TimeWindow tw = track.asTimeWindow();

            minTime = Time.earliest(minTime, tw.start());
            maxTime = Time.latest(maxTime, tw.end());
        }

        return TimeWindow.of(minTime, maxTime);
    }

    /**
     * Create a new Track by parsing all the NOP RH messages found within a single file.
     *
     * @param clazz    Use this class's ClassLoader to locate a resource
     * @param fileName The name of a resource file
     *
     * @return A Track that contains the Points found within the given sourceFile
     */
    public static Track<NopHit> createTrackFromResource(Class clazz, String fileName) {

        Optional<File> trackFile = FileUtils.getResourceAsFile(clazz, fileName);

        if (trackFile.isEmpty()) {
            throw new IllegalStateException("could not find resource: " + fileName);
        }

        return createTrackFromFile(trackFile.get());
    }

    /**
     * Create a new Track by parsing all the NOP RH messages found within a single file.
     *
     * @param sourceFile A file containing NOP data
     *
     * @return A Track that contains the Point found within the given sourceFile
     */
    public static Track<NopHit> createTrackFromFile(File sourceFile) {

        PointIterator ptIter = new PointIterator(new NopParser(sourceFile));

        List<Point<NopHit>> points = newArrayList(ptIter);

        return Track.of(points);
    }

    /**
     * Approximate the total amount of time two Tracks spend within a fixed distance of each other.
     * The result of this method is computed using numeric integration. Providing a smaller time
     * step is will increase the accuracy of the result but it will also increase the time required
     * to compute the result.
     *
     * @param t1       The 1st track
     * @param t2       The 2nd track
     * @param timeStep How often the tracks are tested to see if they are "close" together. Use a
     *                 smaller value to increase accuracy.
     * @param distInNm The maximum distance that meets the definition of "in close proximity"
     *
     * @return An approximation of the amount of time these two tracks spent in close proximity to
     *     one another.
     */
    public static <T> Duration computeTimeInCloseProximity(Track<T> t1, Track<T> t2, Duration timeStep, double distInNm) {

        TimeWindow overlap = t1.getOverlapWith(t2).get();
        long endTimeInEpochMs = overlap.end().toEpochMilli();

        Duration totalTimeCloseTogether = Duration.ZERO;

        Instant currentTime = overlap.start();
        boolean wasClose;
        boolean isClose = false;

        while (currentTime.toEpochMilli() < endTimeInEpochMs) {
            wasClose = isClose;

            //determine if the track "are close right now"
            Optional<Point<T>> opt1 = t1.interpolatedPoint(currentTime);
            Optional<Point<T>> opt2 = t2.interpolatedPoint(currentTime);

            //set the "isClose" variable
            if (opt1.isEmpty() || opt2.isEmpty()) {
                isClose = false;
            } else {
                Point<T> p1 = opt1.get();
                Point<T> p2 = opt2.get();
                double curDist = p1.distanceInNmTo(p2);
                isClose = curDist <= distInNm;
            }

            if (wasClose && isClose) {
                totalTimeCloseTogether = totalTimeCloseTogether.plus(timeStep);
            }

            currentTime = currentTime.plus(timeStep);
        }

        return totalTimeCloseTogether;
    }
}
