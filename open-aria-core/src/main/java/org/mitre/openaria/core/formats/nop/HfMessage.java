package org.mitre.openaria.core.formats.nop;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.formats.nop.NopMessageType.HF_MESSAGE;


/**
 * I do not know what an "HF message" is. An explanation of this message type is not included in the
 * documentation I can find. But, I have seen examples of these messages in my data.
 * <p>
 * This type is included for completeness
 */
public class HfMessage implements NopMessage {

    private final String message;

    public HfMessage(String rawTextInput) {

        checkArgument(HF_MESSAGE.accepts(rawTextInput));

        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return NopMessageType.HF_MESSAGE;
    }
}
