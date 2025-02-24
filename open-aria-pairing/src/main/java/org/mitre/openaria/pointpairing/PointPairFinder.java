
package org.mitre.openaria.pointpairing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.Time;
import org.mitre.caasd.commons.collect.DistanceMetric;
import org.mitre.caasd.commons.collect.MetricTree;
import org.mitre.caasd.commons.collect.SearchResult;
import org.mitre.openaria.core.Point;

/**
 * A PointPairFinder receives Point data from a time-sorted stream of Points. As the Point data
 * comes in the PointPairFinder identifies Pairs of Points that are "close together".
 * <p>
 * The DistanceMetric provided at construction defines exactly how to measure the distance between
 * two Points. The distance threshold provided at construction defines exactly what it means to be
 * "close together".
 * <p>
 * Anytime a "close Pair of Points" is found it is passed to the consumer provided at construction.
 * This consumer can be configured to suit the user. Possible uses include: (A) consumers that
 * perform additional filtering, (B) consumers that perform logging, (C) consumers that publish
 * pairs to a database or other external system, and (D) consumers that are actually a chain of
 * multiple consumers like A->B->C.
 * <p>
 * Points provided via the "accept(Point)" method are automatically added to a specialized data
 * structure (a MetricTree) that efficient finds close pairs. To prevent OutOfMemoryExceptions only
 * a small "time slice" of Point data is kept in memory.
 * <p>
 * Filtering input Points using an ApproximateTimeSorter and/or a StrictTimeSortEnforcer may be
 * helpful because the input stream of Points MUST be sorted by time.
 */
public class PointPairFinder implements Consumer<Point> {

    /**
     * Any pair of points less than this distance apart is passed to the Consumer.
     */
    private final double DISTANCE_THRESHOLD;

    /**
     * This MetricTree (A) defines how to measure distance between Points, and (B) efficiently finds
     * Points that are "close together". Importantly, the Values in this Metric Tree are ignored.
     */
    private MetricTree<Point, Object> mTree;

    /**
     * The TIME_WINDOW governs how much data should be retained at any one time. Point data outside
     * the current TIME_WINDOW is considered stale and will be removed.
     */
    private final Duration TIME_WINDOW;// = Duration.of(13, ChronoUnit.SECONDS);

    private Instant currentTime;

    private final Consumer<Pair<Point, Point>> outputMechanism;

    /*
     * The highest number of Points ever held in this PointPairFinder's mTree. This value gives
     * insight into how much memory is used by the PointPairFinder.
     */
    private int sizeHighWaterMark = 0;

    /**
     * Remove stale data and rebalance the mTree when input data newer than this Instant starts
     * appearing.
     */
    private Instant nextTreeRebuild;

    /**
     * Create a PointPairFinder that absorbs Point data and passes any Pair of "close Points" to a
     * Consumer.
     *
     * @param metric          A DistanceMetric that defines how to measure distance between Points.
     * @param threshold       The distance at which two Points are considered "close together". Any
     *                        pair with a distance measurement the falls below this threshold is
     *                        passed to the consumer.
     * @param outputMechanism A consumer that will be "fed" any Pair of Points that are identified
     */
    public PointPairFinder(Duration timeWindow, DistanceMetric<Point> metric, double threshold, Consumer<Pair<Point, Point>> outputMechanism) {

        checkNotNull(timeWindow);
        checkArgument(timeWindow.toMillis() > 100);
        checkNotNull(metric, "The DistanceMetric cannot be null");
        checkArgument(threshold >= 0, "The distance threshold must be non-negative");
        checkArgument(!Double.isNaN(threshold), "The distance threshold must be a number");
        checkNotNull(outputMechanism, "The Point Pair Consumer cannot be null");

        this.TIME_WINDOW = timeWindow;
        this.mTree = new MetricTree<>(metric);
        this.DISTANCE_THRESHOLD = threshold;
        this.outputMechanism = outputMechanism;
    }

    /**
     * Create a PointPairFinder that absorbs Point data and passes any Pair of "close Points" to a
     * Consumer. This constructor uses the DistanceMetric and pair threshold provided by the
     * property object.
     *
     * @param props           A property object which bundles the properties necessary to configure
     *                        a PointDistanceMetric and the pairing threshold that distance metric
     *                        should use.
     * @param outputMechanism A consumer that will be "fed" any Pair of Points that are identified
     */
    public PointPairFinder(PairingConfig props, Consumer<Pair<Point, Point>> outputMechanism) {
        this(props.timeWindow(), props.distMetric(), props.pairingThreshold(), outputMechanism);
    }

    /**
     * @return The number of Points currently stored in this
     */
    public int size() {
        return this.mTree.size();
    }

    /**
     * @return The highest number of Point Pairs ever held in this PointPairFinder's storage. This
     *     value gives insight into how much memory is used by the PointPairFinder.
     */
    public int sizeHighWaterMark() {
        return this.sizeHighWaterMark;
    }

    /**
     * Trigger multiple operations: (1) Find all points in the retained collection of point data
     * that are in range of the input Point "newPoint", (2) Add the "newPoint" to the collection of
     * point data, (3) "push" all Pairs found in step 1 to the Consumer provided at construction,
     * (4) consider removing stale data from the working collection of point data.
     *
     * @param newPoint A new piece of Point data
     */
    @Override
    public void accept(Point newPoint) {

        updateTimeAndConfirmOrdering(newPoint.time());

        List<SearchResult<Point, Object>> pointsWithinRange = mTree.getAllWithinRange(
            newPoint,
            DISTANCE_THRESHOLD
        );

        //add after search so the "newPoint" isn't in the pointsWithinRange data
        mTree.put(newPoint, null);

        makeAndPublishPairs(pointsWithinRange, newPoint);

        periodicallyPerformCleanUp(newPoint.time());

        sizeHighWaterMark = Math.max(sizeHighWaterMark, this.size());
    }

    private void updateTimeAndConfirmOrdering(Instant candidateTime) {

        if (currentTime == null) {
            currentTime = candidateTime;
            nextTreeRebuild = candidateTime.plus(TIME_WINDOW); //schedule the 1st tree rebuild.
        } else {
            //input data must be sorted
            Time.confirmStrictTimeOrdering(currentTime, candidateTime);
            currentTime = candidateTime;
        }
    }

    private void makeAndPublishPairs(List<SearchResult<Point, Object>> list, Point newPoint) {
        for (SearchResult<Point, Object> searchResult : list) {
            publishOnePair(Pair.of(newPoint, searchResult.key()));
        }
    }

    private void publishOnePair(Pair<Point, Point> pair) {
        outputMechanism.accept(pair);
    }

    private void periodicallyPerformCleanUp(Instant timeOfMostRecentInput) {

        if (timeOfMostRecentInput.isAfter(nextTreeRebuild)) {
            this.nextTreeRebuild = timeOfMostRecentInput.plus(TIME_WINDOW); //schedule next rebuild
            rebuildTree();
        }
    }

    private void rebuildTree() {
        MetricTree<Point, Object> newTree = newTreeWithoutStaleData();
        mTree.clear(); //make it easier to garbage collect the old data
        mTree = newTree;
    }

    private MetricTree<Point, Object> newTreeWithoutStaleData() {

        //build a new tree using the DistanceMetric of the old mTree
        MetricTree<Point, Object> newTree = new MetricTree<>(mTree.metric());

        ArrayList<Point> retainedData = pointsThatAreNotStale();
        //randomize the data we create our tree out of, it produces a better tree
        Collections.shuffle(retainedData);

        for (Point point : retainedData) {
            newTree.put(point, null);
        }
        return newTree;
    }

    private ArrayList<Point> pointsThatAreNotStale() {

        ArrayList<Point> retainedData = new ArrayList<>(mTree.size());

        Instant oldestAllowableTime = currentTime.minus(TIME_WINDOW);

        for (Point point : mTree.keySet()) {
            if (oldestAllowableTime.isBefore(point.time())) {
                retainedData.add(point);
            }
        }
        return retainedData;
    }
}
