package org.mitre.openaria.airborne;

import static org.mitre.openaria.threading.TrackMaking.makeTrackPairFromNopData;
import static org.mitre.caasd.commons.fileutil.FileUtils.getResourceFile;
import static org.mitre.caasd.commons.parsing.nop.Facility.D10;
import static org.mitre.caasd.commons.parsing.nop.Facility.D21;
import static org.mitre.caasd.commons.parsing.nop.NopParsingUtils.parseNopTime;

import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.ScoredInstant;
import org.mitre.openaria.core.TrackPair;

public class EventStatisticsCollectorTest {

    @Test
    public void canMakeReportsWithNoData() {

        EventStatisticsCollector collector = new EventStatisticsCollector();

        //Verify that the report can be made
        String textReport = collector.makeFacilityReport(D10);
    }

    @Test
    public void canMakeSummaryReportWithNoData() {
        EventStatisticsCollector collector = new EventStatisticsCollector();

        //Verify that the report can be made without throwing any NPEs even when data is missing
        String textReport = collector.makeSummaryReport();
    }

    static TrackPair SCARY_TRACK_PAIR = makeTrackPairFromNopData(getResourceFile("scaryTrackData.txt"));

    static ScoredInstant SCORED_INSTANT = new ScoredInstant(
        3.0,
        parseNopTime("03/24/2018", "15:02:59.117")
    );


    @Test
    public void canMakeReportsWithData() {

        EventStatisticsCollector collector = new EventStatisticsCollector();

        collector.accept(new AirborneEvent(SCARY_TRACK_PAIR, SCORED_INSTANT));

        //Verify that the report CAN BE MADE,
        collector.makeFacilityReport(D21);
        collector.makeFacilityReport(D10);
    }

    @Test
    public void canMakeSummaryReportsWithData() {

        EventStatisticsCollector collector = new EventStatisticsCollector();

        collector.accept(
            new AirborneEvent(SCARY_TRACK_PAIR, SCORED_INSTANT)
        );

        //Verify that the report CAN BE MADE
        collector.makeSummaryReport();

        //Manually verify that the report looks nice
        //System.out.println(collector.makeSummaryReport());
    }
}
