
package org.mitre.openaria.smoothing;

import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.Test;
import org.mitre.openaria.core.*;
import org.mitre.caasd.commons.Distance;

public class DuplicateTimeRemoverTest {

    @Test
    public void Clean_FromCleanTrack_ReturnSameTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack clean = trackWithTimes(1, 2, 3, 4, 5);
        assertThat(test.clean(clean).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_FromShortTrack_ReturnSameTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack clean = trackWithTimes(1, 2);
        assertThat(test.clean(clean).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_WithOneDuplicateAndSkip_ReturnCleanTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack dirty = trackWithTimes(1, 2, 2, 4, 5);
        MutableTrack clean = trackWithTimes(1, 2, 4, 5);
        assertThat(test.clean(dirty).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_WithOneDuplicateAndNoSkip_ReturnCleanTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack dirty = trackWithTimes(1, 2, 2, 3, 4);
        MutableTrack clean = trackWithTimes(1, 2, 3, 4);
        assertThat(test.clean(dirty).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_WithOneDuplicateAtStart_ReturnCleanTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack dirty = trackWithTimes(1, 1, 2, 3, 4);
        MutableTrack clean = trackWithTimes(1, 2, 3, 4);
        assertThat(test.clean(dirty).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_WithOneDuplicateAtEnd_ReturnCleanTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack dirty = trackWithTimes(1, 2, 3, 4, 4);
        MutableTrack clean = trackWithTimes(1, 2, 3, 4);
        assertThat(test.clean(dirty).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_WithTwoDuplicates_ReturnCleanTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack dirty = trackWithTimes(1, 1, 3, 4, 4);
        MutableTrack clean = trackWithTimes(1, 3, 4);
        assertThat(test.clean(dirty).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_WithTwoAdjacentDuplicates_ReturnCleanTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack dirty = trackWithTimes(1, 1, 2, 2, 3, 4);
        MutableTrack clean = trackWithTimes(1, 2, 3, 4);
        assertThat(test.clean(dirty).get(), hasSameTimesAs(clean));
    }

    @Test
    public void Clean_WithTripleDuplicate_ReturnCleanTrack() throws Exception {

        DuplicateTimeRemover test = new DuplicateTimeRemover();
        MutableTrack dirty = trackWithTimes(1, 2, 2, 2, 3, 4);
        MutableTrack clean = trackWithTimes(1, 2, 3, 4);
        assertThat(test.clean(dirty).get(), hasSameTimesAs(clean));
    }

    private MutableTrack trackWithTimes(double... times) {

        List<EphemeralPoint> points = DoubleStream.of(times)
            .mapToObj(t -> new PointBuilder()
                .altitude(Distance.ofFeet(Math.random()))
                .latLong(0.0, 0.0)
                .time(Instant.ofEpochMilli((long) (t * 1000)))
                .build())
            .map(pt -> EphemeralPoint.from(pt))
            .toList();

        return new SimpleTrack(points).mutableCopy();
    }

    private Matcher<MutableTrack> hasSameTimesAs(MutableTrack expected) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(MutableTrack actual, Description mismatchDescription) {
                if (expected.size() != actual.size()) {
                    mismatchDescription.appendText("Track did not have correct size.");
                    return false;
                }
                List<Point> expectedPoints = new ArrayList<>(expected.points());
                List<Point> actualPoints = new ArrayList<>(actual.points());
                for (int i = 0; i < expected.size(); i++) {
                    if (!expectedPoints.get(i).time().equals(actualPoints.get(i).time())) {
                        actualPoints.stream()
                            .forEach(p -> mismatchDescription.appendValue((double) p.time().toEpochMilli() / 1000.0).appendText(", "));
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                expected.points().stream()
                    .forEach(p -> description.appendValue((double) p.time().toEpochMilli() / 1000.0).appendText(", "));
            }
        };
    }
}