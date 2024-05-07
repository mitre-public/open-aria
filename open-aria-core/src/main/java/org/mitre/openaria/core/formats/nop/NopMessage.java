package org.mitre.openaria.core.formats.nop;

/**
 * Classes the implement NopMessage should be simple classes that merely relay data found within raw
 * NOP messages like Radar Hit Messages and Flight Plan Messages.
 */
public interface NopMessage {

    NopMessageType getNopType();

    String rawMessage();
}
