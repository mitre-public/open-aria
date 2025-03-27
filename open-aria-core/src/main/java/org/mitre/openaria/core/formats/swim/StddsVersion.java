package org.mitre.openaria.core.formats.swim;


/**
 * Enum represent the known set of STDDS feed versions.  We are currently only adding support for
 * version 4 because prior versions are archival AND more nettlesome which makes parsing them far
 * more complicated.
 */
public enum StddsVersion {
    /**
     * SMES
     */
    SWIM_R31,
    /**
     * SMES/TAIS
     */
    SWIM_R32,
    /**
     * SMES/TAIS
     */
    SWIM_R33,
    /**
     * SMES/TAIS
     */
    SWIM_R40
}
