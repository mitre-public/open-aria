package org.mitre.openaria.core.formats.nop;


public class NopParsingException extends RuntimeException {

    NopParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    NopParsingException(String message) {
        super(message);
    }

    private static final long serialVersionUID = -3351727457440720460L;

}
