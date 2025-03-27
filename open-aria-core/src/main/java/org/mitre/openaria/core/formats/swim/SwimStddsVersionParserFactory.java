package org.mitre.openaria.core.formats.swim;


import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Function;

import org.mitre.swim.parse.JaxbSwimMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class can be used to generate predicates which check whether or not a given namespace (as extracted from a STDDS (TAIS
 * or SMES) xml message) is currently supported by the TDP functional parsers which map:
 * <br>
 * {@link JaxbSwimMessage}s -> {@link TaisPoint}s
 * <br>
 * Currently these factory predicates will indicate support for:
 * <br>
 * 1. R31 - SMES
 * 2. R32 - SMES/TAIS
 * 3. R33 - SMES/TAIS
 * 4. R40 - SMES/TAIS
 * <br>
 * The package-private version-specific parsers live in org.mitre.caasd.ttfs.stdds.{smes, tais} respectively.
 * <br>
 * Note these "parsers" are expecting to receive a cleaned version of the xml namespace as provided in the body of the input
 * message, currently the most reliable way to do this (and how they are used) is via {@link JaxbSwimMessage#getNamespace()}.
 */
public final class SwimStddsVersionParserFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SwimStddsVersionParserFactory.class);

    private SwimStddsVersionParserFactory() {
        throw new IllegalStateException("Unable to instantiate static factory class.");
    }

    public static Function<String, Optional<StddsVersion>> newTaisVersionParser() {
        return SwimStddsVersionParserFactory::taisVersionFromNamespace;
    }

    /**
     * Mapping from namespace format/content to a specific {@link StddsVersion} from a TAIS message.
     */
    private static Optional<StddsVersion> taisVersionFromNamespace(String namespace) {
        requireNonNull(namespace, "Namespace cannot be null.");

        switch (namespace) {
            case "urn:us:gov:dot:faa:atm:terminal:entities:v2-0:tais:terminalautomationinformation":
                return Optional.of(StddsVersion.SWIM_R32);
            case "urn:us:gov:dot:faa:atm:terminal:entities:v3-0:tais:terminalautomationinformation":
                return Optional.of(StddsVersion.SWIM_R33);
            case "urn:us:gov:dot:faa:atm:terminal:entities:v4-0:tais:terminalautomationinformation":
                return Optional.of(StddsVersion.SWIM_R40);
            default:
                return Optional.empty();
        }
    }

    public static Function<String, Optional<StddsVersion>> newSmesVersionParser() {
        return SwimStddsVersionParserFactory::smesVersionFromNamespace;
    }

    /**
     * Mapping from namespace format/content to a specific {@link StddsVersion} from a SMES message.
     */
    private static Optional<StddsVersion> smesVersionFromNamespace(String namespace) {
        switch (namespace) {
            case "urn:us:gov:dot:faa:atm:terminal:entities:smes:surfacemovementevent":
                return Optional.of(StddsVersion.SWIM_R31);
            case "urn:us:gov:dot:faa:atm:terminal:entities:v2-0:smes:surfacemovementevent":
                return Optional.of(StddsVersion.SWIM_R32);
            case "urn:us:gov:dot:faa:atm:terminal:entities:v3-0:smes:surfacemovementevent":
                return Optional.of(StddsVersion.SWIM_R33);
            case "urn:us:gov:dot:faa:atm:terminal:entities:v4-0:smes:surfacemovementevent":
                return Optional.of(StddsVersion.SWIM_R40);
            default:
                return Optional.empty();
        }
    }
}
