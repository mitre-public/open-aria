
package org.mitre.openaria.smoothing;

import java.util.Objects;
import java.util.Optional;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointField;
import org.mitre.caasd.commons.DataCleaner;

public class SpeedExtrapolator implements DataCleaner<MutableTrack> {

    /**
     * Create a cleaned version of the track with null speeds at the beginning of the track replaced
     * by the first nonnull speed.  This data cleaner is intended for Asdex data which is delta
     * encoded.  Once the delta encoding is resolved, the only points which could still have missing
     * speeds are at the start of the track.
     *
     * @param track A Track
     *
     * @return A cleaned track whose first points have speeds extrapolated from the first present
     *     speed.
     */
    @Override
    public Optional<MutableTrack> clean(MutableTrack track) {

        Optional<Double> firstSpeed = track.points().stream().map(Point::speedInKnots).filter(Objects::nonNull).findFirst();

        if (!firstSpeed.isPresent()) {
            return Optional.empty();
        }

        for (MutablePoint point : track.points()) {
            if (point.speedInKnots() == null) {
                point.set(PointField.SPEED, firstSpeed.get());
            } else {
                break;
            }
        }

        return Optional.of(track);
    }
}
