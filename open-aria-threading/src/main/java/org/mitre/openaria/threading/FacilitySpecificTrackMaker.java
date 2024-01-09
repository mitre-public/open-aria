
package org.mitre.openaria.threading;

import java.time.Duration;
import java.util.function.Consumer;

import org.mitre.openaria.core.ApproximateTimeSorter;
import org.mitre.openaria.core.FacilitySpecificDataIngestor;
import org.mitre.openaria.core.Track;
import org.mitre.caasd.commons.parsing.nop.Facility;

public class FacilitySpecificTrackMaker {

    FacilitySpecificDataIngestor ingestor;

    Consumer<Track> outputMechanism;

    public FacilitySpecificTrackMaker(String dirOfManyNopFiles, Consumer<Track> outputMechanism) {
        this.ingestor = new FacilitySpecificDataIngestor(dirOfManyNopFiles);
        this.outputMechanism = outputMechanism;
    }

    public void makeTracksFor(Facility facility) {

        TrackMaker trackMaker = new TrackMaker(outputMechanism);

        ingestor.processPointsFrom(
            facility,
            new ApproximateTimeSorter(Duration.ofMinutes(15), trackMaker)
        );
    }

    public long numPointsProcessed() {
        return ingestor.numPointsProcessed();
    }
}
