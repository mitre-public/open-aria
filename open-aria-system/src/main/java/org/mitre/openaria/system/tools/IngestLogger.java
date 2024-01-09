
package org.mitre.openaria.system.tools;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static org.mitre.openaria.core.AriaUtils.getFacilityFromFilename;
import static org.mitre.caasd.commons.fileutil.FileUtils.makeDirIfMissing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.mitre.openaria.core.utils.TimeUtils;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.parsing.nop.Facility;

import com.google.common.collect.Lists;

/**
 * When properly supplied with input files an IngestLogger (1) tracks the number
 * of files uploaded,
 * (2) tracks the total bytes uploaded, and (3) detects data outages by noticing
 * when historic
 * upload patterns fall off.
 * <p>
 * Properly supplying the IngestLogger with data requires (A) providing files
 * via the "accept"
 * method and (B) calling the "updateLogs()" method at a fixed rate (say once
 * every 15 minutes).
 */
public class IngestLogger implements Consumer<File> {

    private final String dataOutageDirectory = "dataOutageLogs";

    private final String ingestFilePrefix = "ingestSummary";

    private final Map<Facility, List<IngestSummary>> ingestLogs;

    private Instant loggingSince;

    private final String logDirectory;

    /**
     * Create a new IngestLogger which can "watch" incoming files
     */
    public IngestLogger(String logDirectory) {
        this.ingestLogs = initalizeLogs();
        this.loggingSince = Instant.now();
        this.logDirectory = checkNotNull(logDirectory, "The log directory cannot be null");
    }

    private Map<Facility, List<IngestSummary>> initalizeLogs() {

        HashMap<Facility, List<IngestSummary>> map = new HashMap<>();
        Instant startTime = Instant.now();

        for (Facility facility : Facility.values()) {
            map.put(facility, Lists.newArrayList(new IngestSummary(startTime)));
        }
        return map;
    }

    private IngestSummary getCurrentSummaryFor(Facility facility) {

        List<IngestSummary> ingestTimeSeries = ingestLogs.get(facility);

        // the "working" summary is the last summary in the series
        return ingestTimeSeries.get(ingestTimeSeries.size() - 1);
    }

    @Override
    public void accept(File inputFile) {

        Facility sourceFacility = getFacilityFromFilename(inputFile.getName());

        if (isNull(sourceFacility)) {
            System.out.println(
                    "Could not properly log: " + inputFile.getName()
                            + " because the file name could not be mapped to a specific facility");
            return;
        }

        getCurrentSummaryFor(sourceFacility).incorporateFile(inputFile);
    }

    public void writeReportToFile() {

        FileUtils.makeDirIfMissing(logDirectory);
        String targetFilename = logDirectory + File.separator + ingestFilePrefix + "_"
                + TimeUtils.todaysDateAsString() + ".txt";

        try (FileWriter fw = new FileWriter(new File(targetFilename), true)) {
            fw.append("\nIn the " + durationOfLogging() + " since " + TimeUtils.asString(loggingSince) + ":\n");

            long totalBytes = 0;
            int totalFiles = 0;

            for (Facility facility : Facility.values()) {
                IngestSummary summary = getCurrentSummaryFor(facility);

                String fileCountAsString = String.format("%,d", summary.numFiles);
                String bytesAsString = String.format("%,d", summary.totalBytes);

                fw.append("  " + facility + " had " + fileCountAsString + " Files totaling " + bytesAsString
                        + " bytes\n");

                totalBytes += summary.totalBytes;
                totalFiles += summary.numFiles;
            }

            String numFilesAsString = String.format("%,d", totalFiles);
            String totalBytesAsString = String.format("%,d", totalBytes);

            fw.append("In total uploaded: "
                    + numFilesAsString + " Files totaling "
                    + totalBytesAsString + " bytes "
                    + "since " + TimeUtils.asString(loggingSince) + "\n");

            fw.flush();

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return The amount of time between now and when the current round of logging
     *         began.
     */
    private String durationOfLogging() {
        return TimeUtils.asString(
                Duration.between(
                        loggingSince,
                        Instant.now()));
    }

    public void updateLogs() {
        detectAndLogDataOutages();
        cycleIngestLogs();
    }

    private void detectAndLogDataOutages() {
        for (Facility facility : Facility.values()) {
            if (detectDataOutageIn(ingestLogs.get(facility))) {
                logDataOutageFor(facility);
            }
        }
    }

    /**
     * Detect a data outage in this time series of ingest data
     *
     * @param list
     */
    private boolean detectDataOutageIn(List<IngestSummary> list) {

        // detecting data outages requires a history of at least 5 "ingest measurements"
        if (list.size() < 5) {
            return false;
        }

        int n = list.size() - 1;

        boolean current = list.get(n).hadData();
        boolean prior = list.get(n - 1).hadData();
        boolean curMinus2 = list.get(n - 2).hadData();
        boolean curMinus3 = list.get(n - 3).hadData();
        boolean curMinus4 = list.get(n - 4).hadData();

        boolean allOthers = curMinus2 && curMinus3 && curMinus4;

        /*
         * a data outage is detected when the current and prior logs did not have data
         * but the 3
         * logs before those did
         */
        return (!current && !prior && allOthers);
    }

    private void logDataOutageFor(Facility facility) {

        makeDirIfMissing(dataOutageDirectory);

        String targetFilename = dataOutageDirectory + File.separator
                + "dataOutage_" + facility + "_" + TimeUtils.todaysDateAsString() + ".txt";

        try (FileWriter fw = new FileWriter(new File(targetFilename), true)) {
            fw.append("\nData Outage Detected for Facility: " + facility + "\n");

            List<IngestSummary> list = ingestLogs.get(facility);

            for (IngestSummary summary : list) {

                fw.append(
                        summary.numFiles + " Files "
                                + "totaling " + summary.totalBytes + " bytes "
                                + "at " + TimeUtils.asString(summary.startTime) + "\n");
            }

            fw.flush();

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /*
     * Add a new "working" IngestSummary for each facility.
     *
     * Periodically calling this function, say every 15 minutes, creates a
     * time-series dataset that
     * tracks how many files and how many bytes are uploaded every 15 mintues (for
     * each facility).
     */
    private void cycleIngestLogs() {
        Instant startTime = Instant.now();
        this.loggingSince = startTime;
        for (Facility facility : Facility.values()) {
            ingestLogs.get(facility).add(new IngestSummary(startTime));
        }
    }

    private static class IngestSummary {

        final Instant startTime;
        int numFiles;
        long totalBytes;

        IngestSummary(Instant startTime) {
            this.startTime = startTime;
            this.numFiles = 0;
            this.totalBytes = 0;
        }

        void incorporateFile(File f) {
            numFiles++;
            totalBytes += f.length();
        }

        boolean hadData() {
            return numFiles > 0;
        }
    }
}
