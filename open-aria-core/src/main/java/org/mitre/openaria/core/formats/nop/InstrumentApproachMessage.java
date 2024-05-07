package org.mitre.openaria.core.formats.nop;


import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.formats.nop.NopMessageType.INSTRUMENT_APPROACH_MESSAGE;

public class InstrumentApproachMessage implements NopMessage {

    private final String message;

    public InstrumentApproachMessage(String rawTextInput) {
        checkArgument(INSTRUMENT_APPROACH_MESSAGE.accepts(rawTextInput));
        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return INSTRUMENT_APPROACH_MESSAGE;
    }
}
