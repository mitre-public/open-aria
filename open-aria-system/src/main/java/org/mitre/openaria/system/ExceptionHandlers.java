package org.mitre.openaria.system;


import org.mitre.caasd.commons.util.DailySequentialFileWriter;
import org.mitre.caasd.commons.util.ExceptionHandler;
import org.mitre.caasd.commons.util.SequentialFileWriter;

/**
 * A Collection of static factory methods for various ExceptionHandlers (so people don't have to
 * find different implementations).
 */
public class ExceptionHandlers {

    public static ExceptionHandler printToSystemErr() {
        return new PrintToSystemError();
    }

    public static ExceptionHandler sequentialFileWarner(String targetDirectory) {
        return new SequentialFileWriter(targetDirectory);
    }

    public static ExceptionHandler dailyFileWarner(String targetDirectory) {
        return new DailySequentialFileWriter(targetDirectory);
    }
}
