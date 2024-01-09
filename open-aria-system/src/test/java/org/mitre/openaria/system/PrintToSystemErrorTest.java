
package org.mitre.openaria.system;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PrintToSystemErrorTest {

    //BEFORE TESTING THIS CLASS REDIRECT System.err TO A PRIVATE STREAM WE CAN ACCESS
    private static PrintStream oldSystemErr;
    private static ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeAll
    public static void setUpClass() {
        oldSystemErr = System.err;

        //redirect System.err to a PrintStream we can inspect
        System.setErr(new PrintStream(errContent, true));
    }

    @AfterAll
    public static void tearDownClass() {
        System.setErr(oldSystemErr);
    }

    @Test
    public void warningsAreSentToSystemErr() {

        PrintToSystemError instance = new PrintToSystemError();

        assertThat(instance.warningCount(), is(0));

        instance.warn("warning message");
        assertThat(errContent.toString(), containsString("WARNING #0 : warning message"));
        assertThat(instance.warningCount(), is(1));

        instance.warn("warning message2");
        assertThat(errContent.toString(), containsString("WARNING #1 : warning message2"));
        assertThat(instance.warningCount(), is(2));
    }

    @Test
    public void exceptionsAreSentToSystemErr() {

        PrintToSystemError instance = new PrintToSystemError();

        assertThat(instance.errorCount(), is(0));

        instance.handle(new ArrayIndexOutOfBoundsException());
        assertThat(errContent.toString(), containsString("ERROR #0"));
        assertThat(errContent.toString(), containsString("ArrayIndexOutOfBoundsException"));
        assertThat(instance.errorCount(), is(1));

        instance.handle("myErrorMessage", new IllegalStateException());
        assertThat(errContent.toString(), containsString("ERROR #1 : myErrorMessage"));
        assertThat(errContent.toString(), containsString("IllegalStateException"));
        assertThat(instance.errorCount(), is(2));
    }
}
