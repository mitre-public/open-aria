package org.mitre.openaria.core.formats.swim;

import static java.util.Objects.requireNonNull;
import static org.mitre.openaria.core.formats.swim.SwimStddsVersionParserFactory.newTaisVersionParser;
import static org.mitre.swim.parse.input.SwimParserReader.unmarshal;

import java.util.Optional;
import java.util.function.Function;

import org.mitre.swim.parse.JaxbSwimMessage;
import org.mitre.swim.parse.input.SwimParserReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SwimStddsMessageUnmarshaller implements Function<String, Optional<JaxbSwimMessage>> {

    private static final Logger LOG = LoggerFactory.getLogger(SwimStddsMessageUnmarshaller.class);

    /**
     * Functional class for extracting the {@link StddsVersion} version from the input XML data.
     */
    private final Function<String, Optional<StddsVersion>> versionParser;

    SwimStddsMessageUnmarshaller(Function<String, Optional<StddsVersion>> versionParser) {
        this.versionParser = requireNonNull(versionParser, "Supplied version parser cannot be null.");
    }

    /**
     * Applies the TDP-specific unmarshallers to the input XML messages - note that in doing so it actually unmarshalls the record
     * twice, once via the {@link SwimParserReader} to get the JMS properties (if attached) and then a second time with our custom
     * namespace bindings.
     */
    @Override
    public Optional<JaxbSwimMessage> apply(String xml) {
        requireNonNull(xml, "Input XML message must be non-null");

        Optional<JaxbSwimMessage> swimMessage = Optional.ofNullable(unmarshal(xml));

        Optional<StddsVersion> version = swimMessage.flatMap(message -> versionParser.apply(message.getNamespace()));

        if (!version.isPresent()) {
            LOG.error("Unsupported STDDS version detected when parsing input xml \n{}", xml);
            throw new IllegalArgumentException("Unsupported version of STDDS data supplied to parser, see logs for message.");
        }

        // the SwimParserReader can occasionally return null - so we wrap the return and filter
        // messages with null internal fields
        return swimMessage.filter(message -> message.getObject() != null);
    }


    /**
     * Returns a new STDDS message unmarshaller for TAIS messages.
     * <br>
     * The provided unmarshaller contains the bindings for:
     * <br>
     * 1. R32
     * 2. R33
     * 3. R40
     * <br>
     * Note this function will throw an {@link IllegalArgumentException} if the input STDDS-TAIS version isn't currently supported.
     */
    public static Function<String, Optional<JaxbSwimMessage>> newTaisMessageUnmarshaller() {
        return new SwimStddsMessageUnmarshaller(newTaisVersionParser());
    }
}
