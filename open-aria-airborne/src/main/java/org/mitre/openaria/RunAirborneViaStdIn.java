

package org.mitre.openaria;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.caasd.commons.util.DemotedException.demote;
import static org.mitre.openaria.core.config.YamlUtils.parseYaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;

import org.mitre.caasd.commons.parsing.nop.NopParser;
import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.system.StreamingKpi;

/**
 * This program ingests raw text directly from standard-in and pipes it into the Airborne ARIA
 * algorithm.
 *
 * <p>Here are two example usages:
 * <pre>{@code
 * gzcat A90_rhMessages.gz | java -cp ARIA-uber.jar org.mitre.openaria.RunAirborneViaStdIn config.yaml
 * }</pre>
 *
 * <p>AND
 *
 * <pre>{@code
 * java -cp ARIA-uber.jar org.mitre.openaria.RunAirborneViaStdIn config.yaml < dataFile.txt
 * }</pre>
 * <p>It is crucial for input data to be piped in via a command line operator like {@literal |} or
 * {@literal <}. The alternative is to launch a separate program that will be responsible for
 * pushing data to the stdin of this process. Using a 2nd program is extremely fragile because this
 * program and the "data feeding" program will race each other. The racing programs will result in
 * unpredicatable stopages because this program halts whenever STDIN has no more input data to
 * provide.
 */
public class RunAirborneViaStdIn {

    /**
     * @param args Exactly 1 argument is required: airborne's config file
     */
    public static void main(String[] args) {
        checkArgument(
            args.length == 1,
            "RunAirborneViaStdIn requires exactly 1 command line arguments: CONFIG_FILE"
        );

        File yamlFile = new File(args[0]);

        AirborneFactory factory = configFromYaml(yamlFile).airborneFactory();
        StreamingKpi<AirbornePairConsumer> streamingKpi = factory.createKpi(null);

        //this Iterator parses System.in and provides Points from exactly one Facility
        Iterator<Point> pointIter = stdInPointIterator();

        System.out.println("Starting ARIA system.");
        pointIter.forEachRemaining(streamingKpi);
        streamingKpi.flush();
        System.out.println("DONE WITH ALL PROCESSING");
    }

    /**
     * @return Create a PointIterator that is based on Strings read in from System.in. This
     *     capability is should be used when using a unix/linux | operator to pipe data to program
     *     as input.
     */
    private static PointIterator stdInPointIterator() {
        /* read in data from stdin */
        BufferedReader f = new BufferedReader(new InputStreamReader(System.in));
        NopParser parser = new NopParser(f);
        return new PointIterator(parser);
    }

    static ViaStdInConfig configFromYaml(File yamlFile) {
        try {
            return parseYaml(yamlFile, ViaStdInConfig.class);
        } catch (Exception ex) {
            throw demote(ex);
        }
    }

    /**
     * This static class could be deleted.  We could use YAML to directly build an
     * AirborneFactory.Builder instead of a RunAirborneOnFile.Config.  This would decrease the
     * indent level by 2 spaces by deleting the airborneConfig field
     *
     * <p>We are keeping this class because (1) [main reason] other executable can use this as a
     * pattern, (2) its not much extra code, and (3) future changes might need this layer for
     * additional config options.
     */
    static class ViaStdInConfig {

        AirborneFactory.Builder airborneConfig;

        public ViaStdInConfig() {
            //build with Yaml...
        }

        AirborneFactory airborneFactory() {
            return airborneConfig.build();
        }
    }
}
