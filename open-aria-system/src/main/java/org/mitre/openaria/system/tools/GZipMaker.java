
package org.mitre.openaria.system.tools;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.Files;

/**
 * A GZipMaker copies the contents of multiple (usually text) files to a single .gz File.
 * <p>
 * The resulting .gz file can be used to do things like (A) archive a day's worth of input data or
 * (B) enable running before/after experiments on a standard data set.
 */
public class GZipMaker implements Consumer<File> {

    private final GZIPOutputStream gzOut;

    //This flag prevents ingesting new files when the target gz is closed.
    private boolean isClosed = false;

    public GZipMaker(File targetFile) {
        checkNotNull(targetFile);
        checkArgument(targetFile.getName().endsWith(".gz"), "The targetFile's name must end with \".gz\"");
        this.gzOut = createGzOutput(targetFile);
    }

    private GZIPOutputStream createGzOutput(File targetFile) {
        GZIPOutputStream out = null;
        try {
            out = new GZIPOutputStream(new FileOutputStream(targetFile));  //FAA-ISSUE -- UNRELEASED RESOURCE (the FOS)
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return out;
    }

    @Override
    public void accept(File file) {
        if (isClosed) {
            throw new IllegalStateException("Cannot accept the file: " + file.getName() + " because this GZipMaker is closed");
        }

        addFileContentsToGz(file); //this can have a non-obvious impact on memory.
    }

    private void addFileContentsToGz(File file) {
        /*
         * FACTS:
         *
         * The implementation of this process is much more subtle than I thought it would be.
         *
         * Calling gzOut.write(byte[]) works as expected, but the length of the byte[] *STRONGLY*
         * impacts both the memory and speed performance of this call. A long byte[], like one
         * obtained by calling Files.toByteArray(file)), gets written very quickly. BUT that large
         * byte[] will also stay in memory for a some time. I strongly suspect this is because the
         * GZIPOutputStream's inner Deflater object assumes each byte[] it receives is a relatively
         * small byte buffer, not an entire file. Consequently, the Deflater leaves a copy of the
         * last byte[] it ingested in *NATIVE* memory.
         *
         * TAKEAWAY: writing large byte[] to the gzOut is very fast, but it also places that byte[]
         * in memory (probably until the gzOut adds another byte[]).
         *
         * DECISION: I have decided to add complete files to the gzOut because:
         *
         * EXPERIMENTAL RESULTS:
         *
         * 1 - Copying the entire file in one step is easily 3 times faster than copying the
         * contents of incoming files line by line.
         *
         * 2 - Pushing large byte[]'s to the GZIPOutputStream is viable if you use 10GB of memory.
         * This experiment loaded the NOP data for all facility from a 170-ish large .gz file.
         *
         * 3 - Pushing file content line-by-long to the GZIPOutputStream is viable using only 3 GB
         * of memory. This experiment loaded the NOP data for all facility from a 170-ish large .gz
         * file. The input .gz files were then read line by line and added to the GZIPOutputStream
         *
         * 4 - The target ARIA system ingests many small files. I am hoping the memory overhead of
         * keeping these small files (one per facility) in native memory will be acceptible.
         */
        try {
            gzOut.write(Files.toByteArray(file));
            gzOut.flush();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void close() {
        this.isClosed = true;

        try {
            gzOut.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
}
