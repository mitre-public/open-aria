package org.mitre.openaria.airborne;

import static com.google.common.collect.Lists.newArrayList;
import static org.mitre.openaria.trackpairing.TrackPairFilters.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.mitre.caasd.commons.CompositeCleaner;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.util.SequentialFileWriter;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.formats.nop.NopEncoder;
import org.mitre.openaria.trackpairing.IsFormationFlight;
import org.mitre.openaria.trackpairing.IsFormationFlight.FormationFilterDefinition;

public class DataCleaning {

    /*
     * Each Facility runs its own RiskMetricTrackPairConsumer. Each has its own DataCleaner. These
     * shared FILTER_PRINTERS ensure the output files don't overwrite one another.
     */
    private static final Map<String, Printer> FILTER_PRINTERS = new TreeMap<>();

    /**
     * Create a DataCleaner that removes TrackPair that are "in formation"
     *
     * @return A formation flight filter
     */
    public static DataCleaner<TrackPair> formationFlightFilter(List<FormationFilterDefinition> defs) {

        ArrayList<DataCleaner<TrackPair>> filters = newArrayList();

        int i = 0;
        for (FormationFilterDefinition definition : defs) {

            IsFormationFlight formationFilter = formationFilter(
                definition.timeRequirement,
                definition.proximityRequirement
            );

            DataCleaner<TrackPair> cleaner = (definition.logRemovedFilter)
                ? removeFormationFlights(formationFilter, printTo("formationFilter" + i))
                : removeFormationFlights(formationFilter);

            filters.add(cleaner);
            i++;
        }

        return new CompositeCleaner(filters);
    }

    public static DataCleaner<TrackPair> requireProximity(Distance lateralDist, Distance verticalDist) {

        return (TrackPair pair) -> pair.comeWithin(lateralDist, verticalDist)
            ? Optional.of(pair)
            : Optional.empty();
    }

    /**
     * Create a DataCleaner that removes TrackPairs that never achieve a required separation (and
     * are thus following the same aircraft)
     *
     * @return A "single aircraft" filter
     */
    public static DataCleaner<TrackPair> requireSeparationFilter(boolean logDuplicateTracks, double requiredDiverganceDistInNM) {
        return (logDuplicateTracks)
            ? requireSeparation(requiredDiverganceDistInNM, printTo("didNotDiverge"))
            : requireSeparation(requiredDiverganceDistInNM);
    }

    /*
     * Create a Printer that will create output files in a specific directory.
     */
    static Printer printTo(String dir) {
        if (FILTER_PRINTERS.containsKey(dir)) {
            return FILTER_PRINTERS.get(dir);
        } else {
            Printer returnMe = new Printer(dir);
            FILTER_PRINTERS.put(dir, returnMe);
            return returnMe;
        }
    }

    /**
     * A Printer generates files like:
     *
     * <p>"inFormation/inFormation_1.txt", "inFormation/inFormation_2.txt",
     * "inFormation/inFormation_3.txt", "inFormation/inFormation_4.txt"...
     */
    private static class Printer implements Consumer<TrackPair> {

        SequentialFileWriter writer;

        NopEncoder nopEncoder;

        String filePrefix;

        Printer(String outputDir) {
            System.out.println("Making Printer for: " + outputDir);
            this.writer = new SequentialFileWriter(outputDir);
            this.nopEncoder = new NopEncoder();
            this.filePrefix = outputDir;
        }

        @Override
        public void accept(TrackPair t) {
            writer.write(
                filePrefix,
                nopEncoder.asRawNop(t.track1()) + nopEncoder.asRawNop(t.track2())
            );
        }
    }
}
