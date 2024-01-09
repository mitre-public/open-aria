package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mitre.openaria.smoothing.FixedStepTrackMaker.fixedStepTrackFrom;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Point;
import org.mitre.openaria.core.PointField;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.DataCleaner;
import org.mitre.caasd.commons.Distance;

public class AltitudeSmoother implements DataCleaner<MutableTrack> {

    private final FixedStepDigitalFilter filter;
    private final Duration timeStep;

    public AltitudeSmoother(Duration timeStep) {
        this(timeStep, new SavitzkyGolayFilter(timeStep.getSeconds()));
    }

    public AltitudeSmoother(Duration timeStep, FixedStepDigitalFilter filter) {
        this.filter = checkNotNull(filter);
        this.timeStep = checkNotNull(timeStep);
    }

    @Override
    public Optional<MutableTrack> clean(MutableTrack mutableTrack) {

        // Return original track if duration is less than 2 * timestep
        if (mutableTrack.asTimeWindow().duration().minus(timeStep.multipliedBy(2)).isNegative()) {
            return Optional.of(mutableTrack);
        }

        MutableTrack fixedStepTrack = fixedStepTrackFrom(mutableTrack, timeStep);

        double[] rawAltitudes = getRawAltitudes(fixedStepTrack);
        smoothAltitudes(fixedStepTrack, rawAltitudes);

        interpolateValuesInSourceTrack(mutableTrack, fixedStepTrack);
        return Optional.of(mutableTrack);
    }

    public void interpolateValuesInSourceTrack(MutableTrack sourceTrack, MutableTrack smoothedTrack) {
        sourceTrack.points().parallelStream().forEach(point -> {
            // For points outside the timestep leave source values
            smoothedTrack.interpolatedPoint(point.time())
                .ifPresent(interpolated -> point.set(PointField.ALTITUDE, interpolated.altitude()));
        });
    }

    public void smoothAltitudes(MutableTrack fixedStepTrack, double[] rawAltitudes) {
        double[] smoothedAlts = this.filter.smooth(rawAltitudes);
        applySmoothedValues(fixedStepTrack, smoothedAlts, PointField.ALTITUDE);
    }

    public void applySmoothedValues(MutableTrack mutableTrack, double[] smoothedValues, PointField field) {
        List<MutablePoint> points = new ArrayList<>(mutableTrack.points());
        for (int i = 0; i < points.size(); ++i) {
            points.get(i).set(field, Distance.ofFeet(smoothedValues[i]));
        }
    }

    public double[] getRawAltitudes(Track track) {
        double[] rawAltitudes = new double[track.size()];

        List<Point> points = new ArrayList<>(track.points());
        for (int i = 0; i < points.size(); ++i) {
            rawAltitudes[i] = points.get(i).altitude().inFeet();
        }

        return rawAltitudes;
    }
}
