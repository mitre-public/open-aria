package org.mitre.openaria.system;

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
 * Office, 7515 Colshire Drive, McLean, VA 22102-7539, (703) 983-6000.
 *
 * Copyright 2019 The MITRE Corporation. All Rights Reserved.
 */

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.fileutil.FileUtils.makeDirIfMissing;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.util.function.Consumer;

import org.mitre.caasd.commons.Functions.ToStringFunction;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.out.JsonFileSink;

/**
 * This Consumer writes every String it accepts to a File on the local file system.
 * <p>
 * This is similar to {@link JsonFileSink} only a FileSink takes String input rather than
 * JsonWritable input.
 *
 * @todo -- Consider adding the sub-directory Strategy that JsonFileSink supports.
 * @todo -- Move this class to Commons
 */
public class FileSink implements Consumer<String> {

    /** The root directory where files documenting each event are placed. */
    private final String outputDirectory;

    /** Generates a file name (not extension) per datum */
    private final ToStringFunction<String> fileNamer;

    /**
     * Create an {@link FileSink} that publishes messages to a File in the given directory. The name
     * of the target file is determined by the {@code fileNamer} strategy.
     *
     * @param outputDirectory The output directory of choice
     * @param fileNamer       The function for naming the target file.
     */
    public FileSink(String outputDirectory, ToStringFunction<String> fileNamer) {
        this.outputDirectory = outputDirectory;
        this.fileNamer = checkNotNull(fileNamer, "The file-naming function cannot be null");
    }

    @Override
    public void accept(String message) {

        makeDirIfMissing(outputDirectory);
        String fileName = fileNamer.apply(message);
        String fullFileName = outputDirectory + File.separator + fileName + ".txt";

        try {
            //every message gets a new line (just like System.out.println(msg))
            FileUtils.appendToFile(fullFileName, message + "\n");
        } catch (Exception ex) {
            throw demote("Error writing message to FileSink, fileName = " + fileName, ex);
        }
    }
}
