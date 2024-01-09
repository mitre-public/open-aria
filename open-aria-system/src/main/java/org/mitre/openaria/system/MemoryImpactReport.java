
package org.mitre.openaria.system;

import static java.util.Objects.nonNull;

import java.util.Collection;

import org.mitre.openaria.core.ApproximateTimeSorter;
import org.mitre.openaria.pointpairing.PointPairFinder;
import org.mitre.openaria.threading.TrackMaker;

public class MemoryImpactReport {

    boolean hasPointSorter;
    boolean hasTrackMaker;
    boolean hasPairFinder;

    int numPointsInSorter;
    int numPointsInTrackMaker;
    int numTracksInTrackMaker;
    int numPointInPairFinder;

    int highWaterSorter;
    int highWaterPointsInTrackMaker;
    int highWaterTracksInTrackMaker;
    int highWaterPairs;

    long numPointsPublishedByTrackMaker;

    public MemoryImpactReport(StreamingKpi kpi) {
        this(kpi.pointSorter().inMemoryBuffer(), kpi.trackMaker(), kpi.pointPairFinder());
    }

    public MemoryImpactReport(ApproximateTimeSorter pointSorter, TrackMaker trackMaker, PointPairFinder pairFinder) {
        hasPointSorter = nonNull(pointSorter);
        hasTrackMaker = nonNull(trackMaker);
        hasPairFinder = nonNull(pairFinder);

        if (hasPointSorter) {
            numPointsInSorter = pointSorter.numRecordsInQueue();
            highWaterSorter = pointSorter.sizeHighWaterMark();
        }

        if (hasTrackMaker) {
            numPointsInTrackMaker = trackMaker.currentPointCount();
            numTracksInTrackMaker = trackMaker.numTracksUnderConstruction();
            highWaterPointsInTrackMaker = trackMaker.sizeHighWaterMark();
            highWaterTracksInTrackMaker = trackMaker.numTracksUnderConstructionHighWaterMark();
            numPointsPublishedByTrackMaker = trackMaker.numPointsPublished();
        }

        if (hasPairFinder) {
            numPointInPairFinder = pairFinder.size();
            highWaterPairs = pairFinder.sizeHighWaterMark();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (hasPointSorter) {
            sb.append("\n  Points in point sorter: ").append(numPointsInSorter)
                .append("  (").append(highWaterSorter).append(")");
        }
        if (hasTrackMaker) {
            sb.append("\n  Points in track maker : ").append(numPointsInTrackMaker)
                .append("  (").append(highWaterPointsInTrackMaker).append(")");

            sb.append("\n  Tracks in track maker : ").append(numTracksInTrackMaker)
                .append("  (").append(highWaterTracksInTrackMaker).append(")");

        }
        if (hasPairFinder) {
            sb.append("\n  Points in pair finder : ").append(numPointInPairFinder)
                .append("  (").append(highWaterPairs).append(")");
        }
        return sb.toString();
    }

    public static long totalPointsInSorters(Collection<MemoryImpactReport> reports) {
        int total = 0;
        for (MemoryImpactReport report : reports) {
            total += report.numPointsInSorter;
        }
        return total;
    }

    public static long totalPointsInTrackMakers(Collection<MemoryImpactReport> reports) {
        int total = 0;
        for (MemoryImpactReport report : reports) {
            total += report.numPointsInTrackMaker;
        }
        return total;
    }

    public static long totalPointsPublishedByTrackMakers(Collection<MemoryImpactReport> reports) {
        int total = 0;
        for (MemoryImpactReport report : reports) {
            total += report.numPointsPublishedByTrackMaker;
        }
        return total;
    }

    public static long totalPointsInPairFinders(Collection<MemoryImpactReport> reports) {
        int total = 0;
        for (MemoryImpactReport report : reports) {
            total += report.numPointInPairFinder;
        }
        return total;
    }

}
