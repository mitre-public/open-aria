package org.mitre.openaria.core.formats.nop;


import java.io.Serial;

public class NopParsingException extends RuntimeException {

    NopParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    NopParsingException(String message) {
        super(message);
    }

    @Serial
    private static final long serialVersionUID = -3351727457440720460L;

}
