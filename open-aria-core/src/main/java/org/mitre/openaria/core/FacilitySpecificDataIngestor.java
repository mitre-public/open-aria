
package org.mitre.openaria.core;

import java.io.File;
import java.util.function.Consumer;

import org.mitre.caasd.commons.parsing.nop.Facility;
import org.mitre.caasd.commons.parsing.nop.NopParser;

/**
 * A FacilitySpecificPointProcessor allows you to process all the point data contained within a
 * specific directory for a single Facility.
 */
public class FacilitySpecificDataIngestor {

    private final String dirOfManyNopFiles;

    private long numPointsFound;

    public FacilitySpecificDataIngestor(String dirOfManyNopFiles) {
        this.dirOfManyNopFiles = dirOfManyNopFiles;
        this.numPointsFound = 0L;
    }

    public void processPointsFrom(Facility facility, Consumer<Point> pointConsumer) {

        File directoryAsFile = new File(dirOfManyNopFiles);
        File[] nopFiles = directoryAsFile.listFiles();

        for (File nopFile : nopFiles) {

            if (shouldSkip(nopFile, facility)) {
                continue;
            }
            processFile(nopFile, pointConsumer);
        }
    }

    private boolean shouldSkip(File nopFile, Facility desiredFacility) {

        //skip OS files...
        if (nopFile.getName().startsWith(".")) {
            return true;
        }

        return !AriaUtils.fileIsFromFacility(
            nopFile.getName(),
            desiredFacility
        );
    }

    private void processFile(File nopFile, Consumer<Point> pointConsumer) {

        NopParser parser = new NopParser(nopFile);
        PointIterator iter = new PointIterator(parser);

        while (iter.hasNext()) {
            Point next = iter.next();
            numPointsFound++;
            pointConsumer.accept(next);
        }
    }

    public long numPointsProcessed() {
        return this.numPointsFound;
    }
}
