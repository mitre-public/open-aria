package org.mitre.openaria.airborne.exe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toCollection;
import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.core.TrackPair;

import com.google.common.io.Files;

public class Experiment {

    public final String directoryOfData;

    private final AirbornePairConsumer riskMetricAlgorithm;

    /**
     * Create an Experiment that pipes TrackPairs to a RiskMetricTrackPairConsumer
     *
     * @param directoryOfData Where the raw .trf files can be found
     * @param riskMetric      The riskMetric that will receive each of the TrackPair found in the
     *                        directory of data.
     */
    public Experiment(String directoryOfData, AirbornePairConsumer riskMetric) {
        this.directoryOfData = checkNotNull(directoryOfData);
        this.riskMetricAlgorithm = checkNotNull(riskMetric);
    }

    public void runExperiment() {

        System.out.println("Searching for raw track data in: " + directoryOfData);

        List<File> files = getUsableFiles();

        System.out.println("Found: " + files.size() + " files to evaluate");

        evaluateAllFiles(files);
    }

    private ArrayList<File> getUsableFiles() {
        List<File> filesInDirectory = newArrayList((new File(directoryOfData)).listFiles());

        return filesInDirectory.stream()
            .filter(f -> !f.getName().startsWith(".")) //remove system files like .DS_Store
            .collect(toCollection(ArrayList::new));
    }

    private void evaluateAllFiles(List<File> files) {
        files.stream().forEach(file -> scoreOneEvent(file));
    }

    private void scoreOneEvent(File file) {
        System.out.print("\n" + file.getName() + "\n");
        try {
            TrackPair rawInput = makeTrackPairFromNopData(file);
            riskMetricAlgorithm.accept(rawInput);
        } catch (IllegalStateException ise) {
            if (ise.getMessage().contains("Only intended for files with exactly 2 flights")) {
                System.out.println("FLAW");
                moveFlawedFile(file);
            }
        }
    }

    private void moveFlawedFile(File flawedFile) {

        String DESTINATION_DIR = "/Users/jiparker/Desktop/flawedFiles";

        File fileInHolding = new File(DESTINATION_DIR + File.separator + flawedFile.getName());

        try {
            Files.move(
                flawedFile,
                fileInHolding
            );

        } catch (IOException ioe) {
            System.out.println("ERROR -- COULD NOT MOVE FILE TO HOLDING");
            throw new RuntimeException("Could not move File to holding", ioe);
        }
    }

}
