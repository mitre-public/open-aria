package org.mitre.openaria.airborne;

import java.util.HashMap;
import java.util.Map;

import org.mitre.caasd.commons.parsing.nop.Facility;

/**
 * Mapping of airport towers to facilities designed to manually set the event's isInsideAirspace flag to true if the mapping
 * exists in thie enum.
 * 
 * List provided by WSA, CSA, and ESA then compiled here
 */
public enum TowerFacilityPair {

    WJF(Facility.SCT),
    VCV(Facility.SCT),
    OXR(Facility.SBA),
    CMA(Facility.SBA),    
    VGT(Facility.LAS),
    RAP(Facility.ZDV),
    DHN(Facility.A80),
    ECP(Facility.P31),
    EWN(Facility.ILM),
    EYW(Facility.MIA),
    GTR(Facility.BHM),
    HXD(Facility.SAV),
    ISO(Facility.ILM),
    SBY(Facility.PCT),
    GFK(Facility.ZMP),
    MHK(Facility.ZKC),
    WDG(Facility.OKC),
    GEU(Facility.P50),
    GYR(Facility.P50),
    LAF(Facility.IND),
    MOT(Facility.ZMP);

    private static final Map<String, TowerFacilityPair> stringToEnum = new HashMap<>();
    private final Facility facility;

    static {
        for(TowerFacilityPair p : values()) {
            stringToEnum.put(p.toString(), p);
        }
    }

    TowerFacilityPair(Facility facility){
        this.facility = facility;
    }

    public Facility getFacilityWithBestView() {
        return facility;
    }

    public static TowerFacilityPair fromString(String towerCode) {
        TowerFacilityPair tower = stringToEnum.get(towerCode);
        return tower;
    }

}
