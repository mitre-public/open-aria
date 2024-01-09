
package org.mitre.openaria.core.metadata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Optional;

import org.mitre.caasd.commons.actype.AircraftTypeMapping;
import org.mitre.caasd.commons.actype.models.AircraftClass;
import org.mitre.caasd.commons.actype.models.AircraftType;
import org.mitre.caasd.commons.actype.models.EngineType;
import org.mitre.caasd.commons.actype.models.PilotSystem;

import com.google.common.collect.ImmutableMap;

/**
 * The {@code SimplifiedAircraftTypeMapping} provides ARIA with a reduced/simplified mapping from
 * type designators (e.g. B738) to categories like: aircraft class (e.g. FIXED_WING), engine type
 * (e.g . JET), pilot system (e.g. MANNED), and whether or not it is military (T/F).
 * <p>
 * This is mainly used in "tagging" events when publishing to CEDAR as EORs/NQEs.
 * <p>
 * All methods are null-safe.
 */
public class SimplifiedAircraftTypeMapping {

    private final AircraftTypeMapping mapping;

    public SimplifiedAircraftTypeMapping() {
        this(AircraftTypeMapping.fromCacheFile());
    }

    public SimplifiedAircraftTypeMapping(AircraftTypeMapping mapping) {
        this.mapping = checkNotNull(mapping);
    }

    /**
     * @return the {@link AircraftType} data class mapped to the designator, or {@code null} if the
     *     designator does not map to any known data class.
     */
    public AircraftType fromDesignator(String designator) {
        return mapping.acType(designator)
            .orElse(null);
    }

    /** @return "FIXED_WING", "ROTORCRAFT", "OTHER", or "UNKNOWN" */
    public String bucketedAircraftClass(String designator) {
        return mapping.acType(designator)
            .map(AircraftType::getAircraftClass)
            .map(REDUCED_CLASS_MAP::get)
            .orElse("UNKNOWN");

        //FLAW -- why don't we get the acType then delegate to "bucketedAircraftClass(AircraftType actual)"
    }

    /** @return "FIXED_WING", "ROTORCRAFT", "OTHER", or "UNKNOWN" */
    public String bucketedAircraftClass(AircraftType actual) {
        return Optional.ofNullable(actual)
            .map(AircraftType::getAircraftClass)
            .map(REDUCED_CLASS_MAP::get)
            .orElse("UNKNOWN");
    }

    /** @return "PISTON", "TURBOPROP", "JET", "OTHER", or "UNKNOWN" */
    public String bucketedEngineType(String designator) {

        return Optional.ofNullable(designator)
            .flatMap(mapping::acType)
            .map(AircraftType::getEngineType)
            .map(REDUCED_ENGINE_MAP::get)
            .orElse("UNKNOWN");

        //FLAW -- why don't we get the acType then delegate to "bucketedEngineType(AircraftType actual)"
    }

    /** @return "PISTON", "TURBOPROP", "JET", "OTHER", or "UNKNOWN" */
    public String bucketedEngineType(AircraftType actual) {

        return Optional.ofNullable(actual)
            .map(AircraftType::getEngineType)
            .map(REDUCED_ENGINE_MAP::get)
            .orElse("UNKNOWN");
    }

    /** @return "MANNED", "NOT_MANNED", or "UNKNOWN" */
    public String bucketedPilotSystem(String designator) {

        return Optional.ofNullable(designator)
            .flatMap(mapping::acType)
            .map(AircraftType::getPilotSystem)
            .map(REDUCED_PILOT_MAP::get)
            .orElse("UNKNOWN");
    }

    /** @return "MANNED", "NOT_MANNED", or "UNKNOWN" */
    public String bucketedPilotSystem(AircraftType actual) {

        return Optional.ofNullable(actual)
            .map(AircraftType::getPilotSystem)
            .map(REDUCED_PILOT_MAP::get)
            .orElse("UNKNOWN");
    }

    /** @return "T" (true), "F" (false), or "U" (unknown) */
    public String isMilitary(String designator) {

        return Optional.ofNullable(designator)
            .flatMap(mapping::acType)
            .map(t -> t.isMilitary() ? "TRUE" : "FALSE")
            .orElse("UNKNOWN");
    }

    /** @return "T" (true), "F" (false), or "U" (unknown) */
    public String isMilitary(AircraftType actual) {

        return Optional.ofNullable(actual)
            .map(t -> t.isMilitary() ? "TRUE" : "FALSE")
            .orElse("UNKNOWN");
    }

    private static final Map<AircraftClass, String> REDUCED_CLASS_MAP = ImmutableMap.<AircraftClass, String>builder()
        .put(AircraftClass.FIXED_WING_LAND, "FIXED_WING")
        .put(AircraftClass.FIXED_WING_AMPHIBIAN, "FIXED_WING")
        .put(AircraftClass.FIXED_WING_SEAPLANE, "FIXED_WING")
        .put(AircraftClass.ROTOCRAFT, "ROTORCRAFT")
        .put(AircraftClass.GYROCOPTER, "ROTORCRAFT")
        .put(AircraftClass.HELICOPTER, "ROTORCRAFT")
        .put(AircraftClass.TILT_ROTOR, "ROTORCRAFT")
        .put(AircraftClass.TILT_WING, "ROTORCRAFT")
        .put(AircraftClass.DIRIGIBLE, "OTHER")
        .put(AircraftClass.BALOON, "OTHER")
        .put(AircraftClass.POWERED_PARACHUTE, "OTHER")
        .put(AircraftClass.GLIDER, "OTHER")
        .put(AircraftClass.WEIGHT_SHIFT_CONTROL, "OTHER")
        .build();

    private static final Map<EngineType, String> REDUCED_ENGINE_MAP = ImmutableMap.<EngineType, String>builder()
        .put(EngineType.PISTON, "PISTON")
        .put(EngineType.TWO_CYCLE, "PISTON")
        .put(EngineType.FOUR_CYCLE, "PISTON")
        .put(EngineType.ROTARY, "PISTON")
        .put(EngineType.TURBOPROP_TURBOSHAFT, "TURBOPROP")
        .put(EngineType.TURBO_PROP, "TURBOPROP")
        .put(EngineType.TURBO_SHAFT, "TURBOPROP")
        .put(EngineType.JET, "JET")
        .put(EngineType.TURBO_JET, "JET")
        .put(EngineType.TURBO_FAN, "JET")
        .put(EngineType.RAMJET, "JET")
        .put(EngineType.ELECTRIC, "OTHER")
        .put(EngineType.ROCKET, "OTHER")
        .put(EngineType.NONE, "OTHER")
        .build();

    private static final Map<PilotSystem, String> REDUCED_PILOT_MAP = ImmutableMap.<PilotSystem, String>builder()
        .put(PilotSystem.MANNED, "MANNED")
        .put(PilotSystem.OPTIONALLY_PILOTED, "NOT_MANNED")
        .put(PilotSystem.REMOTELY_PILOTED, "NOT_MANNED")
        .put(PilotSystem.AUTONOMOUS_UNMANNED, "NOT_MANNED")
        .build();

}
