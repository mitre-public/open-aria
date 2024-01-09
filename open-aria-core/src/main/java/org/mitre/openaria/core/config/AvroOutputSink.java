package org.mitre.openaria.core.config;

import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.config.YamlUtils.requireMapKeys;
import static org.mitre.caasd.commons.util.DemotedException.demote;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.mitre.caasd.commons.out.OutputSink;


/**
 * An AvroOutputSink writes all received input to an avro file.
 */
public class AvroOutputSink<T> implements OutputSink<T> {

    private final Schema schema;

    private final DataFileWriter<T> dataFileWriter;

    private int recordCount;

    private String sinkFileName;

    public AvroOutputSink(Class<T> typeToArchive, String sinkFileName) {
        requireNonNull(typeToArchive);
        requireNonNull(sinkFileName);

        this.sinkFileName = sinkFileName;
        this.schema = ReflectData.get().getSchema(typeToArchive);
        this.dataFileWriter = new DataFileWriter<T>(
            new ReflectDatumWriter<>(schema)
        );
        //Hard coding the codec like this is bad.  This should be moved to a config option...
        CodecFactory cf = CodecFactory.deflateCodec(CodecFactory.DEFAULT_DEFLATE_LEVEL);
        requireNonNull(cf, "Codec could not be found");  //Using snappy compression without adding Snappy to the classpath produces a NPE here..
        dataFileWriter.setCodec(cf);
    }


    @Override
    public void accept(T record) {

        if (recordCount == 0) {
            initializeTargetFile();
        }

        recordCount++;

        try {
            dataFileWriter.append(record);
        } catch (IOException ioe) {
            throw demote(ioe);
        }
    }

    @Override
    public void flush() throws IOException {
        dataFileWriter.flush();
    }

    @Override
    public void close() throws IOException {
        System.out.println("Closing: " + this.sinkFileName + " after writing " + recordCount + " records");
        dataFileWriter.flush();
        dataFileWriter.close();
    }

    public Schema schema() {
        return schema;
    }

    public String sinkFileName() {
        return this.sinkFileName;
    }

    public int recordCount() {
        return this.recordCount;
    }

    private void initializeTargetFile() {

        System.out.println("Opening: " + this.sinkFileName + " for data archiving");
        try {
            dataFileWriter.create(schema, new File(sinkFileName));
        } catch (IOException ioe) {
            throw demote(ioe);
        }
    }

    /** This Supplier is designed for YAML-based OutputSink creation. */
    public static class InnerSupplier implements Supplier<AvroOutputSink>, YamlConfigured {

        String archiveClass;

        String archiveFileName;

        public InnerSupplier() {
            //build with yaml
        }

        @Override
        public AvroOutputSink get() {
            requireNonNull(archiveClass);
            requireNonNull(archiveFileName);

            try {
                Class<?> clazz = Class.forName(archiveClass);
                return new AvroOutputSink(clazz, archiveFileName);

            } catch(ClassNotFoundException cnfe) {
                throw demote(cnfe);
            }
        }


        @Override
        public void configure(Map<String, ?> configs) {
            //@todo -- make this logging...
            System.out.println("Applying Configuration mapping to an AvroOutputSink$InnerSupplier");

            requireMapKeys(configs, "archiveClass", "archiveFileName");
            this.archiveClass = (String) configs.get("archiveClass");
            this.archiveFileName = (String) configs.get("archiveFileName");
        }
    }
}
