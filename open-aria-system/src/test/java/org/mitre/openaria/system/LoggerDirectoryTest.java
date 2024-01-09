package org.mitre.openaria.system;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.utils.TimeUtils;
import org.mitre.openaria.system.tools.IngestLogger;
import org.mitre.openaria.system.tools.MemoryImpactLogger;
import org.mitre.caasd.commons.fileutil.FileUtils;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class LoggerDirectoryTest {

    private final String INGEST_TEST_DIRECTORY = "IngestTestDirectory";
    private final String MEMORY_TEST_DIRECTORY = "MemoryTestDirectory";
    private final String DISK_TEST_DIRECTORY = "DiskTestDirectory";
    private final String HOLDING_DIRECTORY = "HoldingTestDirectory";
    private File expectFile_B, expectFile_C, expectFile_D;
    private ConcurrentHashMap<Facility, StreamingKpi> testKpi;

    @BeforeEach
    public void setUp() {

        expectFile_B = new File(INGEST_TEST_DIRECTORY + File.separator + "ingestSummary" + "_"
            + TimeUtils.todaysDateAsString() + ".txt");
        expectFile_C = new File(MEMORY_TEST_DIRECTORY + File.separator + "memoryImpactReport_"
            + TimeUtils.todaysDateAsString() + ".txt");
        expectFile_D = new File(DISK_TEST_DIRECTORY + File.separator + "diskspace_"
            + TimeUtils.todaysDateAsString() + ".txt");
        FileUtils.makeDirIfMissing(HOLDING_DIRECTORY);

        testKpi = new ConcurrentHashMap<>();
    }

    @Test
    public void ingestLoggerThrowsNPE() {
        assertThrows(
            NullPointerException.class,
            () -> new IngestLogger(null)
        );
    }

    @Test
    public void memoryImpactLoggerThrowsNPE() {
        assertThrows(
            NullPointerException.class,
            () -> new MemoryImpactLogger(testKpi, null)
        );
    }

    @AfterEach
    public void teardown() throws IOException {
        try {
            deleteFile(INGEST_TEST_DIRECTORY, expectFile_B);
            deleteFile(MEMORY_TEST_DIRECTORY, expectFile_C);
            deleteFile(DISK_TEST_DIRECTORY, expectFile_D);
        } catch (IOException ioe) {
            throw demote(ioe);
        }
        File f = new File(HOLDING_DIRECTORY);
        Files.delete(f.toPath());
    }

    private void deleteFile(String dir, File f) throws IOException {
        File d = new File(dir);
        Files.deleteIfExists(f.toPath());
        Files.deleteIfExists(d.toPath());
    }
}
