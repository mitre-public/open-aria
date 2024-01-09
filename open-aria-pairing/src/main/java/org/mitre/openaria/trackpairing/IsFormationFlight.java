
package org.mitre.openaria.trackpairing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static org.mitre.openaria.core.TrackPairs.overlapInTime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.mitre.openaria.core.Track;
import org.mitre.openaria.core.TrackPair;
import org.mitre.openaria.core.Tracks;
import org.mitre.caasd.commons.Distance;

/**
 * This Predicate determine if two Aircraft are "in formation".
 * <p>
 * A TrackPair is classified as being "in formation" if the two aircraft spend too much time too
 * close together.
 */
public class IsFormationFlight implements Predicate<TrackPair> {

    private final Duration REQUIRED_FORMATION_TIME;
    private final Distance DISTANCE_THRESHOLD_IN_NM;
    private final Duration TIME_STEP;

    public IsFormationFlight(Duration requiredTimeInFormation, Distance distInNm) {
        this.REQUIRED_FORMATION_TIME = requiredTimeInFormation;
        this.DISTANCE_THRESHOLD_IN_NM = distInNm;
        this.TIME_STEP = Duration.ofSeconds(2);
    }

    @Override
    public boolean test(TrackPair t) {
        return tracksAreInFormation(t.track1(), t.track2());
    }

    public boolean tracksAreInFormation(Track track1, Track track2) {
        checkNotNull(track1, "The 1st input track cannot be null");
        checkNotNull(track2, "The 2nd input track cannot be null");

        //when the tracks do not overlap in time they are not in formation
        if (!overlapInTime(track1, track2)) {
            return false;
        }

        Duration totalTimeInCloseProximity = Tracks.computeTimeInCloseProximity(
            track1,
            track2,
            TIME_STEP,
            DISTANCE_THRESHOLD_IN_NM.inNauticalMiles()
        );

        return totalTimeInCloseProximity.toMillis() > REQUIRED_FORMATION_TIME.toMillis();
    }

    /**
     * @param definitions Formatting: "FILTER_DEF_1|FILTER_DEF_2|FILTER_DEF_3"
     *
     * @return A
     */
    public static List<FormationFilterDefinition> parseMultipleFilterDefs(String definitions) {

        String[] defs = definitions.split("[|]");  //split on the '|' character
        ArrayList<FormationFilterDefinition> list = newArrayList();
        for (String def : defs) {
            list.add(parseFormationFilterDefinition(def));
        }
        return list;
    }

    /**
     * @param definition Formatting: "DISTANCE_REQUIREMENT_IN_NM,TIME_REQUIREMENT_IN_SEC,SHOULD_LOG"
     *                   For example: "0.5,60,false"
     *
     * @return
     */
    public static FormationFilterDefinition parseFormationFilterDefinition(String definition) {

        String[] tokens = definition.split(",");

        Distance dist = Distance.ofNauticalMiles(parseDouble(tokens[0]));
        Duration time = Duration.ofSeconds(parseLong(tokens[1]));
        boolean log = Boolean.parseBoolean(tokens[2]);

        return new FormationFilterDefinition(dist, time, log);
    }

    public static class FormationFilterDefinition {

        public final Distance proximityRequirement;
        public final Duration timeRequirement;
        public final boolean logRemovedFilter;

        public FormationFilterDefinition(Distance proximityReq, Duration timeReq, boolean log) {
            checkNotNull(timeReq);
            checkNotNull(proximityReq);
            this.timeRequirement = timeReq;
            this.proximityRequirement = proximityReq;
            this.logRemovedFilter = log;
        }

        @Override
        public String toString() {
            return proximityRequirement.toString(2) + " " + timeRequirement.getSeconds() + " " + logRemovedFilter;
        }
    }
}
