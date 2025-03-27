package org.mitre.openaria.core.formats.swim;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Function;

import org.mitre.swim.parse.JaxbSwimMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functional class for converting an input unmarshalled {@link JaxbSwimMessage} into a collection of contained {@link TaisPoint}
 * records.
 * <br>
 * This class should be functional across multiple versions of TAIS messages. Currently supported versions are:
 * <br>
 * 3. R 4.0 - {@link SwimTaisR40PointParser}
 */
public final class SwimTaisPointParser implements Function<JaxbSwimMessage, List<TaisPoint>> {

    private static final Logger LOG = LoggerFactory.getLogger(SwimTaisPointParser.class);

    // static parser implementations (In theory, we could eventually support 3.1, 3.2, and 3.3)
    private static final SwimTaisR40PointParser r40Parser = new SwimTaisR40PointParser();

    @Override
    public List<TaisPoint> apply(JaxbSwimMessage jaxbSwimMessage) {
        requireNonNull(jaxbSwimMessage, "Input JaxbSwimMessage to the parser cannot be null.");

        List<TaisPoint> allPoints = newArrayList();

        allPoints.addAll(r40Parser.apply(jaxbSwimMessage));
        LOG.debug("Total points after applying the R40 parser {}", allPoints.size());

        return allPoints;
    }
}
