package org.mitre.openaria.core.formats.nop;


import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.formats.nop.NopMessageType.HANDOFF_MESSAGE;


public class HandOffMessage implements NopMessage {

    private final String message;

    public HandOffMessage(String rawTextInput) {

        checkArgument(HANDOFF_MESSAGE.accepts(rawTextInput));

        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return HANDOFF_MESSAGE;
    }
}
