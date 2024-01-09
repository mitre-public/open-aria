package org.mitre.openaria.airborne;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mitre.openaria.airborne.AirborneProducerRecordFactory.extractHeaders;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;

import java.io.FileReader;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;

public class AirborneProducerRecordFactoryTest {

    @Test
    public void kafkaHeaderContainCorrectSchemaNumber() throws Exception {

        AirborneEvent event = AirborneEvent.parse(
            new FileReader(getResourceFile("scaryTrackOutput.json"))
        );

        Header[] headers = extractHeaders(event);

        Predicate<Header> isSchemaVerHeader = header -> header.key().equals("schemaVer");

        boolean thereIsASchemaVersionHeader = Stream.of(headers).anyMatch(isSchemaVerHeader);

        assertThat(thereIsASchemaVersionHeader, is(true));

        byte[] schemaBytes = Stream.of(headers).filter(isSchemaVerHeader)
            .findFirst()
            .get()
            .value();

        //Eventually, this schema number will change, when that happens we'll update this test
        assertThat(schemaBytes, is("3".getBytes()));
    }
}
