package org.mitre.openaria.smoothing;

import static org.mitre.openaria.core.PointField.ALTITUDE;

import java.util.Optional;

import org.mitre.openaria.core.MutableTrack;
import org.mitre.caasd.commons.DataCleaner;

public class ZeroAltitudeToNull implements DataCleaner<MutableTrack> {
    @Override
    public Optional<MutableTrack> clean(MutableTrack mutableTrack) {

        mutableTrack.points().stream()
            .filter(mp -> !mp.altitudeIsMissing())
            .filter(mp -> mp.altitude().inFeet() <= 0.0)
            .forEach(mp -> mp.set(ALTITUDE, null));

        return Optional.of(mutableTrack);
    }
}
