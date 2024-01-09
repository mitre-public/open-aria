package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.mitre.openaria.core.EphemeralPoint;
import org.mitre.openaria.core.MutablePoint;
import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Track;

/**
 * Convert a track at a variable time step to a fixed interval. Does not modify the original track
 */
public class FixedStepTrackMaker {

    static final Duration DEFAULT_STEP_SIZE = Duration.ofSeconds(3);

    private final Duration stepSize;

    FixedStepTrackMaker(Duration stepSize) {
        checkArgument(stepSize.getSeconds() > 0, "step size must be strictly positive");
        this.stepSize = checkNotNull(stepSize);
    }

    public MutableTrack cloneInFixedStep(Track track) {
        List<Instant> pointTimes = track.asTimeWindow().steppedIteration(this.stepSize);

        // Remove last point to preserve fixed interval. Final point will be unsmoothed
        if (trackHasRemainder(track)) {
            pointTimes = pointTimes.subList(0, pointTimes.size() - 1);
        }

        List<MutablePoint> points = pointTimes.parallelStream()
            .map(time -> EphemeralPoint.from(track.interpolatedPoint(time).orElseThrow(RuntimeException::new)))
            .collect(Collectors.toList());

        return new MutableTrack(points);
    }

    /**
     * Check if timesteps to not fit evenly into track duration and we need to trim off or
     * extrapolate last point
     *
     * @param track track to use for duration
     */
    public boolean trackHasRemainder(Track track) {
        long trackDurationSeconds = track.asTimeWindow().duration().getSeconds();
        long stepSizeSeconds = this.stepSize.getSeconds();
        return trackDurationSeconds % stepSizeSeconds != 0;
    }

    public static MutableTrack fixedStepTrackFrom(Track track, Duration stepSize) {
        FixedStepTrackMaker trackMaker = new FixedStepTrackMaker(stepSize);
        return trackMaker.cloneInFixedStep(track);
    }

    public static MutableTrack fixedStepTrackFrom(Track track) {
        return fixedStepTrackFrom(track, DEFAULT_STEP_SIZE);
    }
}
