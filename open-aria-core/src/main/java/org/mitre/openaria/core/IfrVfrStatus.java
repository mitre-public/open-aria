
package org.mitre.openaria.core;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

public enum IfrVfrStatus {

    IFR, //Instramented Flight Rules
    VFR; //Visual Flight Rules

    private static final Set<String> IFR_VALUES = newHashSet("IFR", "OTP");
    private static final Set<String> VFR_VALUES = newHashSet("VFR");

    /**
     * Convert a String, which typically comes directly from raw NOP data, to an IfrVfrStatus enum
     * value. This method will convert "OTP" (i.e. VFR-on-top codes) to "IFR". The OTP designation
     * is rare enough that it isn't being promoted to a full fledged enum value because the OTP
     * designation is technically an IFR designation (despite the name "VFR-on-top"). This guidance
     * was provided directly from a knowledgeable FAA employee.
     *
     * @param input text
     *
     * @return An IfrVfrStatus
     */
    public static IfrVfrStatus from(String input) {
        /*
         * Directly calling trim().toUpperCase() causes a false positve Security Vunerability due to
         * "Locale Dependent Comparision" CWE-474 / APSC-DV-002520.
         */
        String cleanInput = sanitize(input);

        if (IFR_VALUES.contains(cleanInput)) {
            return IFR;
        } else if (VFR_VALUES.contains(cleanInput)) {
            return VFR;
        } else {
            throw new IllegalArgumentException("No IfrVfrStatus enum value corresponds to: " + input);
        }
    }

    private static String sanitize(String input) {
        return input.trim().toUpperCase();
    }
}
