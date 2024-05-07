package org.mitre.openaria.core.formats.nop;

import static org.mitre.openaria.core.formats.nop.NopMessageType.HEART_BEAT;

import com.google.common.base.Preconditions;

public class HeartBeat implements NopMessage {

    private final String message;

    public HeartBeat(String rawTextInput) {

        Preconditions.checkArgument(HEART_BEAT.accepts(rawTextInput));

        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return NopMessageType.HEART_BEAT;
    }
}
