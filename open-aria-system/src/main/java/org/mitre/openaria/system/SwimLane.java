/*
 * NOTICE:
 * This is the copyright work of The MITRE Corporation, and was produced for the
 * U. S. Government under Contract Number DTFAWA-10-C-00080, and is subject to
 * Federal Aviation Administration Acquisition Management System Clause 3.5-13,
 * Rights In Data-General, Alt. III and Alt. IV (Oct. 1996).
 *
 * No other use other than that granted to the U. S. Government, or to those
 * acting on behalf of the U. S. Government, under that Clause is authorized
 * without the express written permission of The MITRE Corporation. For further
 * information, please contact The MITRE Corporation, Contracts Management
 * Office, 7515 Colshire Drive, McLean, VA  22102-7539, (703) 983-6000.
 *
 * Copyright 2021 The MITRE Corporation. All Rights Reserved.
 */
package org.mitre.openaria.system;

import static org.mitre.caasd.commons.fileutil.FileUtils.writeToNewFile;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.mitre.openaria.core.Point;

import com.google.common.collect.Lists;

/**
 * A SwimLane combines a Queue of data (usually taken from Kafka) and the KPI
 * that will eventually
 * process that data.
 */
public class SwimLane {

    private static AtomicLong failCounter = new AtomicLong(0);
    private static long failBreadCrumbTrigger = 1;
    static String OVER_FLOW_FILEPREFIX = "WARNING-swimlane-overflow-";

    private final BlockingQueue<Point> queue;
    private final StreamingKpi kpi;

    private int numPointsIngested;
    private int numPointsProcessed;

    /**
     * Use this flag to schedule a "complete flush" the next time
     * "processQueuedData" is called.
     * This is necessary when a Kafka Rebalance Event leaves a SwimLane with no more
     * incoming data.
     * Old data (in the KPI) will get stranded when no more data arrives to
     * gradually evict the data
     * held in the KPI's buffer.
     */
    private boolean flushOnNextExecution;

    public SwimLane(StreamingKpi kpi, int capacity) {
        this.kpi = kpi;
        /*
         * The amount of data in a SwimLane queue will vary widely. Sometimes a queues
         * will contain
         * NO data (because a SwimLane is not being used), other times a queue will
         * contain 100%
         * of the data (because it is the only SwimLane being used). Consequently, we DO
         * NOT use an
         * ArrayBlockingQueue because we don't want to allocate space for several big
         * arrays (1 per
         * swim lane) that are frequently empty by design
         */
        this.queue = new LinkedBlockingQueue<>(capacity);
        this.flushOnNextExecution = false;
    }

    public void offerToQueue(Point p) {
        // @todo -- THIS IS FLAWED, this call WILL drop point data when the queue is
        // full
        // WE CANNOT (???) BLOCK (i.e. swap to queue.put(p)) BECAUSE THE "DATA LOADING
        // THREAD" IS THE SAME AS THE "DATA PROCESSING THREAD"
        // ARE YOU SURE? The one data pulling task would block at offerToQueue while the
        // other threads would continue processing data and emptying queues
        boolean addedToQueue = queue.offer(p);

        numPointsIngested++;

        if (!addedToQueue) {
            long curCount = failCounter.incrementAndGet();

            if (curCount == failBreadCrumbTrigger) {
                makeOverflowBreadcrumb(failBreadCrumbTrigger);
                failBreadCrumbTrigger *= 10;

                throw new IllegalStateException("Failed to add Point to SwimLane " + failBreadCrumbTrigger / 10);
            }
        }
    }

    private void makeOverflowBreadcrumb(long failBreadCrumbTrigger) {
        try {
            File target = new File(OVER_FLOW_FILEPREFIX + failBreadCrumbTrigger + ".txt");
            String content = "SwimLanes are dropping data!\n" + failBreadCrumbTrigger + " points have been dropped";

            System.out.println(target.getAbsolutePath());

            writeToNewFile(target, content);
        } catch (IOException ioe) {
            throw demote(ioe);
        }
    }

    public void processQueuedData() {
        // drain the current queue to a separate List so that we process a well-defined
        // bite of data.
        // We don't want to setup an infinite race between the data provider and the kpi
        List<Point> dataToProcess = Lists.newArrayList();
        queue.drainTo(dataToProcess);

        int batchSize = dataToProcess.size();

        dataToProcess.forEach(kpi);

        numPointsProcessed += batchSize;

        flushIfNecessary();
    }

    /**
     * Schedule a "complete flush" the next time "processQueuedData" is called.
     *
     * <p>
     * This is necessary when a Kafka Rebalance Event triggers and the expected "end
     * state" is
     * that a SwimLane will no longer receive incoming data (because some other Node
     * is now
     * processing this partition). When this occurs, we want to schedule a flush
     * because old data
     * within the KPI will be stranded now that there is no more data arriving to
     * gradually evict
     * the data held in the KPI's buffer. However, we want this flush to occur
     * safely
     */
    public void scheduleFlushOnNextExecution() {
        flushOnNextExecution = true;
    }

    private void flushIfNecessary() {
        if (flushOnNextExecution) {
            kpi.flush();
            flushOnNextExecution = false;
        }
    }

    public StreamingKpi kpi() {
        return this.kpi;
    }

    public int queueSize() {
        return queue.size();
    }

    public int numPointsIngested() {
        return numPointsIngested;
    }

    public int numPointsProcessed() {
        return numPointsProcessed;
    }
}
