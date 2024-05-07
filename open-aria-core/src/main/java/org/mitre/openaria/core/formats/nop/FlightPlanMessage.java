package org.mitre.openaria.core.formats.nop;


import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.formats.nop.NopMessageType.FLIGHT_PLAN;

public class FlightPlanMessage implements NopMessage {

    private final String message;

    public FlightPlanMessage(String rawTextInput) {
        checkArgument(FLIGHT_PLAN.accepts(rawTextInput));
        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return FLIGHT_PLAN;
    }
}
