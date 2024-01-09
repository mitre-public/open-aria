
package org.mitre.openaria;

/**
 * This Enum represents the collection of Key Performance Indicators (KPIs) that are able to accept
 * streaming radar data.
 */
public enum AriaKpi {

    AIRBORNE,
    SURFACE,
    CFIT;

    public static AriaKpi from(String string) {
        return valueOf(string.toUpperCase().trim());
    }

}
