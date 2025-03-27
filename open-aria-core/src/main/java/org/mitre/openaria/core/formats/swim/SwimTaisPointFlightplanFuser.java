package org.mitre.openaria.core.formats.swim;


import static java.util.Objects.requireNonNull;

import java.util.function.BiFunction;


/**
 * Adds flight plan-specific TAIS fields to the final {@link TaisPoint} record.
 * <br>
 * This implementation is shareable across version-based parser implementations and so has been
 * broken out into its own class.
 */
final class SwimTaisPointFlightplanFuser implements BiFunction<TaisPoint.Builder, TaisFlightplan, TaisPoint.Builder> {

    @Override
    public TaisPoint.Builder apply(TaisPoint.Builder builder, TaisFlightplan taisFlightPlan) {
        requireNonNull(taisFlightPlan, "Cannot add flight plan fields to builder with null flight plan.");
        return builder
            .arrAirport(arrivalAirport(taisFlightPlan))
            .depAirport(departureAirport(taisFlightPlan))
            .scratchpad1(taisFlightPlan.scratchPad1())
            .scratchpad2(taisFlightPlan.scratchPad2())
            .callsign(taisFlightPlan.acid())
            .aircraftType(taisFlightPlan.acType())
            .flightRules(taisFlightPlan.flightRules())
            .keyboard(taisFlightPlan.keyboard())
            .positionSymbol(taisFlightPlan.positionSymbol());
    }

    static String arrivalAirport(TaisFlightplan taisFlightPlan) {
        return taisFlightPlan.type().filter(TaisFlightplan.TypeofFlightType.A::equals).map(t -> taisFlightPlan.exitFix()).orElse(null);
    }

    static String departureAirport(TaisFlightplan taisFlightPlan) {
        return taisFlightPlan.type().filter(t -> TaisFlightplan.TypeofFlightType.D.equals(t) || TaisFlightplan.TypeofFlightType.P.equals(t)).map(t -> taisFlightPlan.entryFix()).orElse(null);
    }
}
