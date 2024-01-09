
package org.mitre.openaria.core;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mitre.caasd.commons.parsing.nop.NopMessage;
import org.mitre.caasd.commons.parsing.nop.NopMessageType;
import org.mitre.caasd.commons.parsing.nop.NopParsingUtils;
import org.mitre.caasd.commons.parsing.nop.NopRadarHit;

public class NopEncoderTest {

    public NopEncoderTest() {
    }

    @Test
    public void testStars() {

        String realStarsMessage = "[RH],STARS,P31_B,07/10/2016,15:07:27.732,N52383,C182,,7102,065,128,217,030.63143,-087.42549,1657,7102,-12.4587,9.2657,1,E,A,P31,,ENE,JKA,1445,JKA,ACT,VFR,,00140,,,,,,S,1,,0,{RH}";

        Point firstPoint = NopPoint.from(realStarsMessage);
        Point secondPoint = encodeAndReparse(firstPoint);

        verifyPointFieldsAreEqual(firstPoint, secondPoint);
    }

    @Test
    public void testCenter() {
        String realCenterMessage = "[RH],Center,ZLA_B,07-10-2016,06:16:35.000,SKW5840,CRJ2,L,4712,110,355,124,33.4922,-118.1300,465,,,,,/,,ZLA_B,,,,D0608,SAN,,IFR,,465,1396392357,LAX,,110//110,,L,1,,,{RH}";

        Point firstPoint = NopPoint.from(realCenterMessage);
        Point secondPoint = encodeAndReparse(firstPoint);

        verifyPointFieldsAreEqual(firstPoint, secondPoint);
    }

    @Test
    public void testAgw() {
        String realAgwMessage = "[RH],AGW,ABI_B,07/12/2016,19:21:19.384,N2233W,PA28,,6276,066,96,266,032.31720,-098.82792,209,6276,42.77,0.59,1,B,2,ABI,L,MWL,ABIA,,ABI,,VFR,,188,69,JEN276015,,00,,S,0,V,0,,125.69,78.2,{RH}";

        Point firstPoint = NopPoint.from(realAgwMessage);
        Point secondPoint = encodeAndReparse(firstPoint);

        verifyPointFieldsAreEqual(firstPoint, secondPoint);
    }

    @Test
    public void testAgw2() {
        String realAgwMessage = "[RH],AGW,CHS,10/18/2016,00:00:08.281,,,,1200,015,93,170,033.14390,-080.22537,147,,-7.8,16.91,,,,CHS,,,,,???,,,,,7464,???,,00,,,1,,0,,72.2,97.84,{RH}";
        Point firstPoint = NopPoint.from(realAgwMessage);
        Point secondPoint = encodeAndReparse(firstPoint);

        verifyPointFieldsAreEqual(firstPoint, secondPoint);
    }

    @Test
    public void testIfMissingInputDataInvalidatesOutput() {
        /*
         * I added this test when trying to track down a possible bug in NopEncoder.
         *
         * The issue is that encoding two tracks "asNop" created a file with more than two tracks.
         *
         * While attempting to see if missing data caused the "trackNumber" field to become
         * misaligned I added this test. The test shows that missing data did not seem to cause the
         * trackId() fields to become flawed.
         */
        //notice syntheticMessage1 is missing a value right before the latitude.
        String syntheticMessage1 = "[RH],STARS,D21,07/08/2017,18:42:01.774,,,,1200,0,0,,42.80514,-83.00373,415,0,16.5832,35.5480,,,,D21,,,,,,,IFR,,,,,,,,,,,,{RH}";
        String syntheticMessage2 = "[RH],STARS,D21,07/08/2017,18:42:06.334,,,,1200,18,122,191,42.80260,-83.00445,415,0,16.5520,35.3957,,,,D21,,,,,,,IFR,,,,,,,,,,,,{RH}";

        Point firstPoint = NopPoint.from(syntheticMessage1);
        Point secondPoint = NopPoint.from(syntheticMessage2);

        assertEquals(
            firstPoint.trackId(), secondPoint.trackId()
        );
        assertEquals(
            firstPoint.trackId(), "415"
        );
    }

    @Test
    public void testFormatTime() {

        /*
         * Below are 3 consecutive ouputs from calling NopEncoder.asNop(Point).
         *
         * In theory, the 3 input points were ordered by their time value's before they were
         * encoded.
         *
         * So, why are these 3 time values are out of order. Notice, the lat/long, speed, and
         * altitude data all seem to suggest that the flawed point is a proper measurement. Other
         * points not shown in this unit test all track along nicely.
         *
         * Why does the middle point have an out-of-order time value when, in theory, they were in
         * time order when these 3 points were encoded?
         *
         * The suspicion was that there was some time value between 18:54:54.037 (the time of the
         * 1st message) and 18:55:02.430 (the time of the 3rd message) that caused the NopEncoder to
         * fail when writing the middle output. In other words, I suspected it was true that the
         * input points were truly sorted by time but when the NopEncoder "wrote them down" the time
         * value was corrupted some way. My guess was that the out-of-time-order result was an
         * artifact of the "convert an Instant to a String" code.
         *
         * This test rejects that suspicion and confirms that all Instants (using a msec resolution)
         * are parsed correctly. Consequently, the source of the out-of-order input is still
         * unexplained.
         */
        //good 1st message = "[RH],STARS,D21,07/08/2017,18:54:54.037,,,,1200,20,86,46,42.52809,-83.17698,1287,0,8.9700,18.9035,,,,D21,,,,,,,IFR,,,,,,,,,,,,{RH}";
        //flawed 2nd message = "[RH],STARS,D21,07/08/2017,16:53:33.088,,,,1200,20,86,46,42.52932,-83.17531,1287,0,9.0442,18.9777,,,,D21,,,,,,,IFR,,,,,,,,,,,,{RH}"
        //good 3rd message = "[RH],STARS,D21,07/08/2017,18:55:02.430,,,,1200,20,86,46,42.53049,-83.17372,1287,0,9.1145,19.0480,,,,D21,,,,,,,IFR,,,,,,,,,,,,{RH}";
        Instant startTime = NopParsingUtils.parseNopTime("07/08/2017", "18:54:54.037");
        Instant lastTime = NopParsingUtils.parseNopTime("07/08/2017", "18:55:02.430");

        Instant currentTime = startTime;

        while (currentTime.isBefore(lastTime)) {

            String instantAsString = NopEncoder.formatTime(currentTime);
            String[] tokens = instantAsString.split(",");

            Instant parsedInstant = NopParsingUtils.parseNopTime(tokens[0], tokens[1]);

            assertEquals(
                parsedInstant,
                currentTime
            );

            //1 Millisecond is the smallest possible step because NOP only lists upto millisecond
            currentTime = currentTime.plus(Duration.ofMillis(1));
        }
    }

    private Point encodeAndReparse(Point p) {
        String pointAsRawString = (new NopEncoder()).asRawNop(p);
        NopMessage secondMessage = NopMessageType.parse(pointAsRawString);
        return NopPoint.from((NopRadarHit) secondMessage);
    }

    private void verifyPointFieldsAreEqual(Point point1, Point point2) {

        for (PointField field : PointField.values()) {
            assertThat(
                "Field: " + field + " should match",
                field.get(point1), is(field.get(point2))
            );
        }
    }
}
