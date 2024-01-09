
package org.mitre.openaria.system.tools;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.system.MemoryImpactReport.totalPointsInPairFinders;
import static org.mitre.openaria.system.MemoryImpactReport.totalPointsInSorters;
import static org.mitre.openaria.system.MemoryImpactReport.totalPointsInTrackMakers;
import static org.mitre.openaria.system.MemoryImpactReport.totalPointsPublishedByTrackMakers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.mitre.openaria.core.utils.TimeUtils;
import org.mitre.openaria.system.MemoryImpactReport;
import org.mitre.openaria.system.StreamingKpi;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class MemoryImpactLogger implements Runnable {

    Map<Facility, StreamingKpi> kpis;

    private final String logDirectory;

    public MemoryImpactLogger(Map<Facility, StreamingKpi> kpis, String logDirectory) {
        this.kpis = kpis;
        this.logDirectory = checkNotNull(logDirectory, "The log directory cannot be null");
    }

    @Override
    public void run() {
        writeMemoryImpactReport();
    }

    private void writeMemoryImpactReport() {

        FileUtils.makeDirIfMissing(logDirectory);
        String TARGET_FILENAME = logDirectory + File.separator + "memoryImpactReport_" + TimeUtils.todaysDateAsString() + ".txt";

        try (FileWriter fw = new FileWriter(new File(TARGET_FILENAME), true)) {

            fw.write(generateMemoryImpactReport());

            fw.flush();

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String generateMemoryImpactReport() {

        StringBuilder buffer = new StringBuilder();

        buffer
            .append("\n\n---------------\n\n")
            .append(TimeUtils.asString(Instant.now()))
            .append("\n");

        Map<Facility, MemoryImpactReport> reports = getAllMemoryImpactReports();

        addImpactByFacility(reports, buffer);
        addTotalImpact(reports, buffer);

        buffer
            .append(memoryStatus())
            .append("\n")
            .append(TimeUtils.asString(Instant.now()))
            .append("\n");

        return buffer.toString();
    }

    private void addImpactByFacility(Map<Facility, MemoryImpactReport> reports, StringBuilder buffer) {
        //iterating over Facility.values (instead of mapEntries) sorts the result list of reports
        for (Facility facility : Facility.values()) {
            MemoryImpactReport report = reports.get(facility);
            buffer.append(statusOf(facility, report));
        }
    }

    private void addTotalImpact(Map<Facility, MemoryImpactReport> reports, StringBuilder buffer) {
        buffer
            .append("\nTotal Points in Point Sorters: ").append(totalPointsInSorters(reports.values()))
            .append("\nTotal Points in Track Makers: ").append(totalPointsInTrackMakers(reports.values()))
            .append("\nTotal Points in Pair Finders: ").append(totalPointsInPairFinders(reports.values()))
            .append("\nTotal Points Published From Track Makers: ").append(totalPointsPublishedByTrackMakers(reports.values()));
    }

    private Map<Facility, MemoryImpactReport> getAllMemoryImpactReports() {

        Map<Facility, MemoryImpactReport> reports = new HashMap<>();

        for (Facility facility : Facility.values()) {

            StreamingKpi streamer = kpis.get(facility);

            reports.put(facility, new MemoryImpactReport(streamer));
        }
        return reports;
    }

    private String memoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        String message1 = "\nused memory : " + (runtime.totalMemory() - runtime.freeMemory());
        String message2 = "\nfree memory : " + runtime.freeMemory();
        String message3 = "\ntotal memory: " + runtime.totalMemory();
        String message4 = "\nmax memory  : " + runtime.maxMemory();

        return message1 + message2 + message3 + message4;
    }

    private String statusOf(Facility factility, MemoryImpactReport report) {
        return "\n" + factility.toString() + ":" + report.toString();
    }
}
