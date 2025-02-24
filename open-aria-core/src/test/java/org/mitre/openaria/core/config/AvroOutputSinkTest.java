package org.mitre.openaria.core.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.reflect.ReflectDatumReader;
import org.junit.jupiter.api.Test;

class AvroOutputSinkTest {

    static class AvroFriendlyPojo {

        final String stringField;

        final int intField;

        public AvroFriendlyPojo() {
            //Avro needs a public no-arg construction
            stringField = null;
            intField = 0;
        }

        public AvroFriendlyPojo(String s, int i) {
            this.stringField = s;
            this.intField = i;
        }
    }

    @Test
    public void avroSinkProducesAvroFileThatCanBeRead() throws Exception {

        AvroFriendlyPojo thing = new AvroFriendlyPojo("hello", 10);

        AvroOutputSink<AvroFriendlyPojo> sink = new AvroOutputSink<>(
            AvroFriendlyPojo.class,
            "aThing.avro"
        );

        File targetAvroFile = new File("aThing.avro");

        assertThat(targetAvroFile.exists(), is(false));

        sink.accept(thing);
        sink.flush();
        sink.close();

        assertThat("We just made an avro file with 1 record!", targetAvroFile.exists(), is(true));

        DataFileReader<AvroFriendlyPojo> reader = new DataFileReader<>(
            targetAvroFile,
            new ReflectDatumReader<>(sink.schema())
        );

        AvroFriendlyPojo recordFromAvroFile = reader.next();

        assertThat("The avro file contained exactly one record", reader.hasNext(), is(false));
        assertThat(recordFromAvroFile.stringField, is("hello"));
        assertThat(recordFromAvroFile.intField, is(10));

        reader.close();
        targetAvroFile.delete();
    }
}