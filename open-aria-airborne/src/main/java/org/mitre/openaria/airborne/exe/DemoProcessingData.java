package org.mitre.openaria.airborne.exe;

import org.mitre.openaria.RunAirborneOnFile;

public class DemoProcessingData {


    public static void main(String[] args) {
        processNopData();
        processCsvData();
    }

    private static void processCsvData() {

        // i.e., CLI ARGS=
        // "-c open-aria-airborne/src/main/resources/sampleCsvConfig.yaml -f open-aria-airborne/src/main/resources/sampleCsvData.txt.gz"
        String[] args_csv = new String[]{
            "-c", "open-aria-airborne/src/main/resources/sampleCsvConfig.yaml",
            "-f", "open-aria-airborne/src/main/resources/sampleCsvData.txt.gz"
        };

        RunAirborneOnFile.main(args_csv);
    }

    public static void processNopData() {

        // i.e., CLI ARGS=
        // "-c open-aria-airborne/src/main/resources/sampleNopConfig.yaml -f open-aria-airborne/src/main/resources/sampleNopData.txt.gz"
        String[] args_nop = new String[]{
            "-c", "open-aria-airborne/src/main/resources/sampleNopConfig.yaml",
            "-f", "open-aria-airborne/src/main/resources/sampleNopData.txt.gz"
        };

        RunAirborneOnFile.main(args_nop);
    }
}
