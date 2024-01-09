
package org.mitre.openaria.smoothing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Optional;

import org.mitre.openaria.core.MutableTrack;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.CompositeCleaner;
import org.mitre.caasd.commons.DataCleaner;

/**
 * A MutableSmoother is an adapter that converts one or more {@literal DataCleaner<MutableTrack>}
 * objects into a single {@literal DataCleaner<Track>}
 */
public class MutableSmoother implements DataCleaner<Track> {

    private final CompositeCleaner<MutableTrack> combinedCleaner;

    public MutableSmoother(List<DataCleaner<MutableTrack>> trackSmoothers) {
        checkNotNull(trackSmoothers);
        this.combinedCleaner = new CompositeCleaner(trackSmoothers);
    }

    public static MutableSmoother of(DataCleaner<MutableTrack>... cleaners) {
        checkNotNull(cleaners);
        return new MutableSmoother(newArrayList(cleaners));
    }

    @Override
    public Optional<Track> clean(Track track) {
        Optional<MutableTrack> cleanedResult = combinedCleaner.clean(track.mutableCopy());

        return (cleanedResult.isPresent())
            ? Optional.of(cleanedResult.get().immutableCopy())
            : Optional.empty();
    }
}
