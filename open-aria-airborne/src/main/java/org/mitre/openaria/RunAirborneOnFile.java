package org.mitre.openaria;

import static com.google.common.base.Preconditions.checkState;
import static org.mitre.caasd.commons.util.DemotedException.demote;
import static org.mitre.openaria.core.config.YamlUtils.parseYaml;

import java.io.File;
import java.util.Iterator;

import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointIterator;
import org.mitre.openaria.core.formats.nop.NopHit;
import org.mitre.openaria.core.formats.nop.NopParser;
import org.mitre.openaria.system.StreamingKpi;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * This executable program runs Airborne ARIA on a single input File.
 *
 * <p>This program requires two command line args: "The input data file" and "The configuration".
 * The configuration file provides: (1) The AirborneAria algorithm config and (2) The names of
 * plugins that know "How to publish AirborneEvents"
 */
public class RunAirborneOnFile {

    /**
     * Example: "java -cp ARIA.jar org.mitre.openaria.RunAirborneOnFile -c config.yaml -f rawData.gz"
     *
     * @param argv "-c CONFIG_FILE", "--config CONFIG_FILE", "-f DATA_FILE", "--file DATA_FILE"
     */
    public static void main(String[] argv) {

        //Use JCommander to parse the CLI args into a useful class
        CommandLineArgs args = parseCommandLineArgs(argv);

        execute(
            args.dataFile,
            configFromYaml(args.yamlConfig)
        );
    }

    static void execute(File dataFile, Config config) {

        Iterator<Point<NopHit>> pointIterator = iteratorFor(dataFile);

        //Pass null because we don't need the AirborneFactory to keep a KPI-to-Facility mapping
        StreamingKpi<AirbornePairConsumer> streamingKpi = (config.airborneFactory()).createKpi(null);

        System.out.println("Starting ARIA system.  Input File = " + dataFile.getName());
        pointIterator.forEachRemaining(streamingKpi);
        streamingKpi.flush();
        System.out.println("DONE PROCESSING: " + dataFile.getName());
    }

    private static Iterator<Point<NopHit>> iteratorFor(File dataFile) {
        return new PointIterator(new NopParser(dataFile));
    }

    private static Config configFromYaml(File yamlFile) {
        try {
            return parseYaml(yamlFile, RunAirborneOnFile.Config.class);
        } catch (Exception ex) {
            throw demote(ex);
        }
    }

    static Config configFromYaml(String yamlContent) {
        try {
            return parseYaml(yamlContent, RunAirborneOnFile.Config.class);
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
    static class Config {

        // @TODO -- FORMAT GOES HERE

        AirborneFactory.Builder airborneConfig;

        public Config() {
            //build with Yaml...
        }

        AirborneFactory airborneFactory() {
            return airborneConfig.build();
        }
    }

    /** Use JCommander command line argument parser utility to create instances of this class. */
    private static class CommandLineArgs {

        @Parameter(names = {"-c", "--config"}, required = true, description = "A yaml config file")
        private String configFileArg;

        @Parameter(names = {"-f", "--file"}, required = true, description = "The file where raw data will be found")
        String fileCmdLineArg;

        File yamlConfig;

        File dataFile;

        //ensure that when JCommander parses all the args the results meet all requirements
        void verifyArguments() {
            //ensure the propertyFile points us to a viable file
            this.yamlConfig = new File(configFileArg);
            this.dataFile = new File(fileCmdLineArg);

            checkState(dataFile.isFile());
            checkState(dataFile.exists());
        }
    }

    /* Use JCommander util to parse the command line args */
    private static CommandLineArgs parseCommandLineArgs(String[] args) {

        CommandLineArgs parsedArgs = new CommandLineArgs();
        JCommander.newBuilder()
            .addObject(parsedArgs)
            .build()
            .parse(args);

        parsedArgs.verifyArguments();

        return parsedArgs;
    }
}
