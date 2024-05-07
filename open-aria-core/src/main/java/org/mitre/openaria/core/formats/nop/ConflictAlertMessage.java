package org.mitre.openaria.core.formats.nop;


import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.formats.nop.NopMessageType.CONFLICT_ALERT_MESSAGE;



public class ConflictAlertMessage implements NopMessage {

    private final String message;

    public ConflictAlertMessage(String rawTextInput) {

        checkArgument(CONFLICT_ALERT_MESSAGE.accepts(rawTextInput));

        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return CONFLICT_ALERT_MESSAGE;
    }
}
