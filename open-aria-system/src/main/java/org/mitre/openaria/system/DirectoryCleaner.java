
package org.mitre.openaria.system;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.mitre.caasd.commons.fileutil.FileUtils;

/**
 * A DirectoryCleaner is a Runnable designed to be run at a regular interval. When repeatedly run a
 * DirectoryCleaner will "watch" a specific directory looking for files that have reached a stagnant
 * size. When stagnant files are found they are passed to an file consumer (which can launch
 * external processing/logging logic) and then deleted.
 */
public class DirectoryCleaner implements Runnable {

    /** The directory being watched. */
    private final File targetDirectory;

    /** The working list of known files and their most recent sizes (in bytes). */
    private final Map<String, Long> filesAndTheirSizes;

    /**
     * User defined "pluggable" logic. This will typically be something like file parsing, file
     * processing, and/or file logging.
     */
    private final Consumer<File> fileConsumer;

    /**
     * @param targetDir    The directory being watched.
     * @param fileConsumer What happens to a stagnant file before it is deleted.
     */
    public DirectoryCleaner(String targetDir, Consumer<File> fileConsumer) {
        this.targetDirectory = new File(checkNotNull(targetDir));
        this.fileConsumer = checkNotNull(fileConsumer);

        //prevent launching a DirectoryCleaner that has no Directory to clean
        checkArgument(targetDirectory.exists(), "The directory does not exist: " + targetDir);

        this.filesAndTheirSizes = new TreeMap<>();
    }

    @Override
    public void run() {

        //print a start message
        System.out.println("Directory Cleaner -- start " + Instant.now().toString());

        List<File> stagnantFiles = findStagnantFiles();
        processAndRemoveRemainingFiles(stagnantFiles);

        //print a stop message
        System.out.println("Directory Cleaner -- stop " + Instant.now().toString());
    }

    /**
     * Find all files that have the same size as the prior iteration.
     *
     * @return A List of Files with stagnant size
     */
    private List<File> findStagnantFiles() {

        List<File> stagnantFiles = new ArrayList<>();

        for (File f : targetDirectory.listFiles()) {

            //skip directories
            if (f.isDirectory()) {
                continue;
            }

            if (fileWasAlreadyObserved(f) && fileSizeIsUnchanged(f)) {
                stagnantFiles.add(f);
            } else {
                saveFilesCurrentSize(f);
            }
        }

        return stagnantFiles;
    }

    private boolean fileWasAlreadyObserved(File f) {
        return filesAndTheirSizes.containsKey(f.getAbsolutePath());
    }

    private boolean fileSizeIsUnchanged(File f) {
        Long priorSize = filesAndTheirSizes.get(f.getAbsolutePath());
        Long currentSize = f.length();

        return priorSize.equals(currentSize);
    }

    private void saveFilesCurrentSize(File f) {

        /*
         * Files that are "due to be written" (i.e. queued up in a big multi-file write job) sit
         * arround at filesize = 0 for a while before being written. We don't want to record the
         * "fake" files because it enables deleting files that are not yet written.
         */
        if (f.length() == 0) {
//			System.out.println("ignoring: " + f.getName() + " because file size = 0");
            return;
        }

        long fileLength = f.length();
        String fullFilePath = f.getAbsolutePath();
        System.out.println(fullFilePath + "'s current size = " + fileLength);
        filesAndTheirSizes.put(fullFilePath, fileLength);
    }

    private void processAndRemoveRemainingFiles(List<File> files) {

        for (File file : files) {
            fileConsumer.accept(file);
            filesAndTheirSizes.remove(file.getAbsolutePath());

            //only delete files that are still in the directory we are responsible for emptying
            if (fileIsStillInTargetDir(file)) {
                handleUnMovedFile(file);
            }
        }
    }

    /*
     * Return true if this files is still in the directory that needs to be cleaned
     */
    private boolean fileIsStillInTargetDir(File file) {
        /*
         * If a physical File is moved by the fileConsumer the File handle this DirectoryCleaner has
         * will be out-dated (i.e. point to a now missing File)
         */
        return file.exists();
    }

    private void handleUnMovedFile(File file) {

        try {
            FileUtils.appendToFile(
                "fileConsumerDidNotMoveFile.txt",
                "The file: " + file.getAbsolutePath() + " is still in the targetDir"
            );
        } catch (Exception ex) {
            throw demote(ex);
        }

        System.out.println("The file: " + file.getAbsolutePath() + " is still in the targetDir");
        file.delete();
    }
}
