package org.mitre.openaria.system;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.system.ExceptionHandlers.sequentialFileWarner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.mitre.caasd.commons.util.ExceptionHandler;
import org.mitre.caasd.commons.util.ShutdownHook;

/**
 * An AriaServiceAssetBundle is a collection of assets that are useful when building a long-running
 * process that is meant to never (or rarely) shutdown.
 *
 * <p>An AriaServiceAssetBundle contains (1) An "mainExecutor" backed by a thread pool that can
 * execute the core work of an AriaService, (2) a single-threaded "fastExecutor" reserved for light,
 * time-sensitive tasks like period logging and sending heartbeat events (3) a sharable
 * ExceptionHandler that can help "harden" an AriaService by providing a shared mechanism to handle
 * all Exceptions that occur while an AriaService is running (thus ensuring the long-running
 * AriaService does not crash easily).
 */
public class AriaServiceAssetBundle {

    /** An ExecutorService reserved for light, time-sensitive tasks (like simple logging). */
    private final ScheduledExecutorService fastExecutor;

    /** An ExecutorService that very well could reach saturation (hence the fastExecutor). */
    private final ScheduledExecutorService eventExecutor;

    /** A ExceptionHandler that can (if you choose) be used to catch all exception */
    private final ExceptionHandler exceptionHandler;

    /**
     * Create an AriaServiceAssetBundle that includes: one single threaded ScheduledExecutorService
     * for "light work", and one ScheduledExecutorService backed by a threadpool for "heavy work".
     *
     * @param numWorkerThreads The size of the threadpool backing the mainExecutor.
     * @param exceptionHandler A shared ExceptionHandler that can be used as a global service
     */
    public AriaServiceAssetBundle(int numWorkerThreads, ExceptionHandler exceptionHandler) {
        checkArgument(numWorkerThreads >= 1);
        checkNotNull(exceptionHandler);
        this.exceptionHandler = exceptionHandler;
        this.fastExecutor = createEventScheduler(1);
        this.eventExecutor = createEventScheduler(numWorkerThreads);
    }


    /**
     * Create an AriaServiceAssetBundle that always uses a SequentialFileWarner as the
     * ExceptionHandler.
     *
     * @param numWorkerThreads The size of the threadpool backing the mainExecutor.
     */
    public AriaServiceAssetBundle(int numWorkerThreads) {
        this(numWorkerThreads, sequentialFileWarner("warnings"));
    }

    private ScheduledExecutorService createEventScheduler(int numThreads) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(numThreads);

        /*
         * Ensure that this never ending process is properly closed when the user presses "Control +
         * C" (not "Control + Z", which will leave some resources stranded)
         */
        ShutdownHook.addShutdownHookFor(executor);

        return executor;
    }

    /**
     * @return The ScheduledExecutorService reserved for all compute-intensive tasks. Scheduling
     *     "light work" should be done via the fastExecutor method.
     */
    public ScheduledExecutorService mainExecutor() {
        return eventExecutor;
    }

    /**
     * @return A single threaded ScheduledExecutorService reserved for "easy to compute" tasks. The
     *     goal of this "fastTaskExecutor" is to ensure "easy to execute" tasks like sending a
     *     heartbeat messages and updating logs do not get needlessly delayed because the
     *     "mainExecutor" is saturated with work. For example, if 4-of-4 threads are spiked to max
     *     CPU usage we still want to be able to update logs with information like "how much memory
     *     is being used" and "how much disk space is available right now"
     */
    public ScheduledExecutorService fastTaskExecutor() {
        return fastExecutor;
    }

    public ExceptionHandler exceptionHandler() {
        return this.exceptionHandler;
    }
}
