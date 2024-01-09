
package org.mitre.openaria.system;

import static com.google.common.base.Preconditions.checkNotNull;

import org.mitre.caasd.commons.util.ExceptionHandler;

public class PrintToSystemError implements ExceptionHandler {

    private int warningCount = 0;
    private int errorCount = 0;

    @Override
    public void warn(String string) {
        checkNotNull(string);
        System.err.println("WARNING #" + warningCount + " : " + string);
        warningCount++;
    }

    @Override
    public void handle(String string, Exception exception) {
        checkNotNull(string);
        checkNotNull(exception);
        System.err.println("ERROR #" + errorCount + " : " + string);
        errorCount++;
        exception.printStackTrace(System.err);
    }

    public int warningCount() {
        return warningCount;
    }

    public int errorCount() {
        return errorCount;
    }
}
