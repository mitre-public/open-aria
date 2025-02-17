package org.mitre.openaria.airborne.exe;

import static org.mitre.openaria.core.config.YamlUtils.parseYaml;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.mitre.openaria.AirborneFactory;
import org.mitre.openaria.airborne.AirbornePairConsumer;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.formats.Format;
import org.mitre.openaria.core.formats.ariacsv.AriaCsvHit;
import org.mitre.openaria.system.StreamingKpi;

/**
 * This Demo show the simplest code for processing raw location data encoded in OpenARIA CSV data.
 * <p>
 * This is intended to provide an easier on-ramp for getting to know the OpenARIA codebase.
 */
public class DemoProcessingCsvData {

    public static void main(String[] args) throws IOException {

        // --- Convert a YAML configuration file to a "data processing pipeline" ---
        File yamlConfig = new File("open-aria-airborne/src/main/resources/demoCsvAirborneFactory.yaml");
        AirborneFactory.Builder builder = parseYaml(yamlConfig, AirborneFactory.Builder.class);
        AirborneFactory airborneFactory = builder.build();
        StreamingKpi<AirbornePairConsumer> dataProcessor = airborneFactory.createKpi(null);
        // The resulting "dataProcessor" will receive a continuous stream of radar data

        // --- Convert a .gz file of "OpenARIA CSV data" into an Iterator of Points (aka parse radar data)
        // This file contains about 20 minutes of radar data (33,500 radar hits)
        // This file contains observations describing 470 different aircraft
        File dataFile = new File("open-aria-airborne/src/main/resources/convertedNop.txt.gz");

        @SuppressWarnings("unchecked") // This cast elicits a warning -- need to improve how Generics are handled
        Format<AriaCsvHit> format = (Format<AriaCsvHit>) airborneFactory.format();
        Iterator<Point<AriaCsvHit>> dataIterator = format.parseFile(dataFile);

        // --- Process the data
        System.out.println("Starting OpenARIA system.  Input File = " + dataFile.getName());
        dataIterator.forEachRemaining(dataProcessor);
        dataProcessor.flush();
        System.out.println("DONE PROCESSING: " + dataFile.getName());
    }
}