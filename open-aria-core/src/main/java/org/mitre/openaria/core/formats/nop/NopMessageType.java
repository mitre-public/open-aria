package org.mitre.openaria.core.formats.nop;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A NopItem is a list of the kinds of content found in raw NOP files
 */
public enum NopMessageType {

    AGW_RADAR_HIT("[RH],AGW,", true) {
        @Override
        NopMessage parseMessage(String line) {
            return new AgwRadarHit(line);
        }
    },
    STARS_RADAR_HIT("[RH],STARS,", true) {
        @Override
        NopMessage parseMessage(String line) {
            return new StarsRadarHit(line);
        }
    },
    CENTER_RADAR_HIT("[RH],Center,", true) {
        @Override
        NopMessage parseMessage(String line) {
            return new CenterRadarHit(line);
        }
    },
    MEARTS_RADAR_HIT("[RH],MEARTS,", true) {
        @Override
        NopMessage parseMessage(String line) {
            return new MeartsRadarHit(line);
        }
    },
    FLIGHT_PLAN("[FP],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new FlightPlanMessage(line);
        }
    },
    HEART_BEAT("[HB],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new HeartBeat(line);
        }
    },
    BYTES_MESSAGE("[Bytes]", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new BytesMessage(line);
        }
    },
    CONFLICT_ALERT_MESSAGE("[CA],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new ConflictAlertMessage(line);
        }
    },
    INSTRUMENT_APPROACH_MESSAGE("[IA],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new InstrumentApproachMessage(line);
        }
    },
    HANDOFF_MESSAGE("[OH],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new HandOffMessage(line);
        }
    },
    HF_MESSAGE("[HF],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new HfMessage(line);
        }
    },
    TRAFFIC_COUNT_MESSAGE("[TC],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new TrafficCountMessage(line);
        }
    },
    SH_MESSAGE("[SH],", false) {
        @Override
        NopMessage parseMessage(String line) {
            return new ShMessage(line);
        }
    };

    /**
     * The short prefix that appears at the beginning of all NOP messages of this particular type.
     */
    private final String messagePrefix;

    private final boolean isRadarHit;

    NopMessageType(String prefix, boolean isRadarHit) {
        this.messagePrefix = prefix;
        this.isRadarHit = isRadarHit;
    }

    /**
     * Convert a line of text to an instance of a class that implements NopMessage
     *
     * @param line A Line of text which should match the NOP Message formatting rules
     *
     * @return An instance of a NopMessage class
     * @throws NopParsingException when the input line either cannot be matched to a NopMessageType
     *                             OR when after matching to a specific NopMessageType the input
     *                             could not be parsed by the corresponding concrete class
     */
    public static NopMessage parse(String line) {
        checkNotNull(line, "Cannot parse a NopMessage from a null String");

        try {
            for (NopMessageType type : NopMessageType.values()) {
                if (type.accepts(line)) {
                    return type.parseMessage(line);
                }
            }
        } catch (Exception ex) {
            throw new NopParsingException("Exception when parsing:\n  " + line, ex);
        }
        throw new NopParsingException("Could not match the input:\n  " + line + "\n to a NopType");
    }

    /**
     * @param line A single line of raw NOP input text
     *
     * @return True if the line begins with the prefix of this know NOP message type
     */
    public boolean accepts(String line) {
        return line.startsWith(messagePrefix);
    }

    public boolean isRadarHit() {
        return this.isRadarHit;
    }

    /**
     * @return True if the input message matches the early formatting expectations of a NOP Radar
     *     Hit.  This is not a strict filter.  But a good first pass.
     */
    public static boolean isNopRadarHit(String message) {
        checkNotNull(message);

        return STARS_RADAR_HIT.accepts(message)
            || CENTER_RADAR_HIT.accepts(message)
            || MEARTS_RADAR_HIT.accepts(message)
            || AGW_RADAR_HIT.accepts(message);
    }

    /**
     * @return The short prefix that appears at the beginning of all NOP messages of this particular
     *     type.
     */
    public String messagePrefix() {
        return messagePrefix;
    }

    /**
     * Convert a line of NOP text to an Object that may (or may not) provide a more convenient API
     * for accessing the data within the message
     *
     * @param line A single line of raw NOP input text
     *
     * @return A concrete object that implements NopFileEntity
     */
    abstract NopMessage parseMessage(String line);
}
