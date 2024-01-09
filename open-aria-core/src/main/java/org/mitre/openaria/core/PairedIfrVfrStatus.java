package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.core.IfrVfrStatus.IFR;
import static org.mitre.openaria.core.IfrVfrStatus.VFR;

import java.util.List;

public enum PairedIfrVfrStatus {

    IFR_IFR, //Two aircraft both under Instramented Flight Rules
    IFR_VFR, //One aircraft under IFR, a second aircraft under VFR
    VFR_VFR; //Two aircraft, both under Visual Flight Rules

    public static PairedIfrVfrStatus from(IfrVfrStatus one, IfrVfrStatus two) {
        checkNotNull(one);
        checkNotNull(two);

        if (one == IFR && two == IFR) {
            return IFR_IFR;
        }

        if (one == VFR && two == VFR) {
            return VFR_VFR;
        }

        return IFR_VFR;
    }

    public static PairedIfrVfrStatus from(List<IfrVfrStatus> twoStatusValues) {
        checkNotNull(twoStatusValues);
        checkArgument(twoStatusValues.size() == 2, "The input list must contain exactly two items");

        return from(twoStatusValues.get(0), twoStatusValues.get(1));
    }
}
