package org.mitre.openaria.core.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mitre.openaria.core.data.TrackRecord.newMutableTrack;
import static org.mitre.openaria.core.data.TrackRecord.newTrackRecord;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;

class TrackRecordTest {

    @Test
    void newImmutableTrackCannotBeEdited() throws Exception {

        File sourceFile = new File("src/test/resources/csvTrack1.txt");

        List<CsvPoint> points = Files.lines(sourceFile.toPath())
            .map(s -> new CsvPoint(s))
            .toList();

        Track immutableTrack = newTrackRecord(points);

        assertThrows(
            UnsupportedOperationException.class,
            () -> immutableTrack.points().pollFirst()
        );
    }

    @Test
    void newMutableTrackCanBeEdited() throws Exception {

        File sourceFile = new File("src/test/resources/csvTrack1.txt");

        List<CsvPoint> points = Files.lines(sourceFile.toPath())
            .map(s -> new CsvPoint(s))
            .toList();

        Track mutableTrack = newMutableTrack(points);

        int sizePre = mutableTrack.size();
        mutableTrack.points().pollFirst();
        int sizePost = mutableTrack.size();

        assertThat(sizePre, is(sizePost + 1));
    }

    @Test
    void asImmutable_cannotEditResult() throws Exception {

        File sourceFile = new File("src/test/resources/csvTrack1.txt");

        List<CsvPoint> points = Files.lines(sourceFile.toPath())
            .map(s -> new CsvPoint(s))
            .toList();

        TrackRecord mutableTrack = newMutableTrack(points);
        mutableTrack.points().pollFirst();  //See...we can edit this track...

        Track immutableTrack = mutableTrack.asImmutable();

        assertThrows(
            UnsupportedOperationException.class,
            () -> immutableTrack.points().pollFirst()
        );
    }

    @Test
    void asMutable_canBeEdited() throws Exception {

        File sourceFile = new File("src/test/resources/csvTrack1.txt");

        List<CsvPoint> points = Files.lines(sourceFile.toPath())
            .map(s -> new CsvPoint(s))
            .toList();

        TrackRecord immutableTrack = newTrackRecord(points);

        assertThrows(
            UnsupportedOperationException.class,
            () -> immutableTrack.points().pollFirst()
        );

        Track mutable  = immutableTrack.asMutable();

        assertDoesNotThrow(() -> mutable.points().pollFirst());
    }

}