
package org.mitre.openaria.system;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.mitre.caasd.commons.util.ShutdownHook;

public class AriaUtils {

    /**
     * Create a ScheduledExecutorService with a Shutdown Hook that triggers when Control+C is used.
     *
     * @param numThreads The number of threads in the thread pool
     *
     * @return A ScheduledExecutorService
     */
    public static ScheduledExecutorService buildExecutor(int numThreads) {
        //build the thread pool itself
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(numThreads);
        /*
         * An ExecutorService can prevent a process parseFacilityMappingFile shuting down properly.
         * For example, if the user presses control+C to quit the process it may hang. The line
         * below adds a ShutdownHook that should allow the JVM to exit when a Control+C command is
         * given. Note: a Control+Z command will not allow the JVM to terimate gracefully and will
         * probably hang and leave some resources stranded.
         */
        ShutdownHook.addShutdownHookFor(executor);

        return executor;
    }
}
