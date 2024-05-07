package org.mitre.openaria.core.formats.nop;


import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.formats.nop.NopMessageType.TRAFFIC_COUNT_MESSAGE;

public class TrafficCountMessage implements NopMessage {

    private final String message;

    public TrafficCountMessage(String rawTextInput) {
        checkArgument(TRAFFIC_COUNT_MESSAGE.accepts(rawTextInput));
        this.message = rawTextInput;
    }

    @Override
    public String rawMessage() {
        return message;
    }

    @Override
    public NopMessageType getNopType() {
        return TRAFFIC_COUNT_MESSAGE;
    }
}
