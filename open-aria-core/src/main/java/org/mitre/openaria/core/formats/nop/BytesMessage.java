package org.mitre.openaria.core.formats.nop;



import static org.mitre.openaria.core.formats.nop.NopMessageType.BYTES_MESSAGE;

import com.google.common.base.Preconditions;

public class BytesMessage implements NopMessage {

    private final String message;

    public BytesMessage(String rawTextInput) {

        Preconditions.checkArgument(BYTES_MESSAGE.accepts(rawTextInput));

        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return BYTES_MESSAGE;
    }
}
