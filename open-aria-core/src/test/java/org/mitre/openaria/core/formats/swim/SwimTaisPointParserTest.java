package org.mitre.openaria.core.formats.swim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.function.Function;

import org.mitre.swim.parse.input.SwimParserReader;

import org.junit.jupiter.api.Test;

class SwimTaisPointParserTest {

    private static final Function<String, String> namespaceExtractor = xml -> SwimParserReader.unmarshal(xml).getNamespace();

    private static final Function<String, Optional<StddsVersion>> taisVersionParser = SwimStddsVersionParserFactory.newTaisVersionParser();


    @Test
    void testParseTaisR40NamespaceTrackFlightplanMessage_Newlined_Namespace() {
        String namespace = namespaceExtractor.apply(SampleTaisMessages.sampleR40TrackFlightplan_namespace_newlined);
        assertEquals(Optional.of(StddsVersion.SWIM_R40), taisVersionParser.apply(namespace), "TrackFlightplan message should return 4.0 version for namespace: ".concat(namespace));
    }

}