package org.mitre.openaria.core.formats.swim;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functional class for converting an input TAIS tracon identifier into a known {@link Tracon} supported by TDP.
 * <br>
 * The Tracon is important as the TAIS track messages come in with positions as X,Y relative to some facility-specific location
 * and the mapping for that location requires a lookup based on Tracon identifier to find the facility position.
 * <br>
 * Some of the TAIS facility names are also slightly different than the standard STARs tracon names and so we go through some
 * light standardization to try and match things up.
 */
public final class SwimTaisTraconResolver implements Function<String, Optional<Tracon>> {

  private static final Logger LOG = LoggerFactory.getLogger(SwimTaisTraconResolver.class);

  @Override
  public Optional<Tracon> apply(String tracon) {
    requireNonNull(tracon, "Supplied Tracon cannot be null for resolution.");

    if (Tracon.TRACON_NAMES.contains(tracon)) {
      return Optional.of(Tracon.valueOf(tracon));
    } else {
      return Optional.empty();  //@todo -- Question?, will we support swim data without a known tracon?

//      Optional<EramFacility> facilityMatch = ArtsFacilityConverter.INSTANCE.bestAvailableLocFacility(
//          tracon,
//          ArtsFacilityConverter.IS_TRACON
//      );
//      LOG.debug("Matched TAIS Tracon ID {} with facility {} in the facility mapping.", tracon, facilityMatch);
//
//      return facilityMatch.map(EramFacility::facilityNop).filter(Tracon.TRACON_NAMES::contains).map(Tracon::valueOf);
    }
  }
}
