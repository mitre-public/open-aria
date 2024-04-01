package org.mitre.openaria.core.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

import org.junit.jupiter.api.Test;

class PointRecordTest {

    @Test
    public void canUseBuilder() {

        double LATITUDE = 12.0;
        double LONGITUDE = -10.0;

        PointRecord<String> pt = new PointRecord.Builder<String>()
            .time(Instant.EPOCH)
            .latLong(LATITUDE, LONGITUDE)
            .build();

        assertThat(pt.time(), is(Instant.EPOCH));
        assertThat(pt.latitude(), is(LATITUDE));
        assertThat(pt.longitude(), is(LONGITUDE));
    }

    @Test
    public void testDemo() {
        PointRecord<String> pt = new PointRecord<>(
            Instant.EPOCH,
            "vinNum",
            LatLong.of(0.0, 1.0),
            Distance.ofFeet(123.0),
            "I am a payload"
        );

        assertThat(pt.time(), is(Instant.EPOCH));
        assertThat(pt.linkId(), is("vinNum"));
        assertThat(pt.latLong(), is(LatLong.of(0.0, 1.0)));
        assertThat(pt.payload(), is("I am a payload"));
        assertThat(pt.asCsvText(), is(",," + Instant.EPOCH +",vinNum,0.0000,1.0000,123"));
    }

    @Test
    public void canBuildStrPointRec() {

        PointRecord<String> pt = new PointRecord.Builder<String>()
            .time(Instant.EPOCH)
            .latLong(0.0, 1.1)
            .payload("hello")
            .build();
    }

    @Test
    public void canBuildByteArrayPointRec() {

        PointRecord<byte[]> pt = new PointRecord.Builder<byte[]>()
            .time(Instant.EPOCH)
            .latLong(0.0, 1.1)
            .payload(new byte[] {(byte)4, (byte)5})
            .build();
    }

    @Test
    public void canReplaceTime() {

        PointRecord<String> pt = new PointRecord.Builder<String>()
            .time(Instant.EPOCH)
            .latLong(0.0, 1.1)
            .payload("hello")
            .build();

        PointRecord<String> pt2 = pt.withTime(Instant.EPOCH.plusMillis(2));

        assertThat(pt2.latLong(), is(pt.latLong()));
        assertThat(pt2.time(), is(not(pt.time())));
        assertThat(pt2.time(), is(Instant.EPOCH.plusMillis(2)));
        assertThat(pt2.linkId(), is(pt.linkId()));
        assertThat(pt.altitude(), is(pt.altitude()));
        assertThat(pt2.rawData(), is(pt.rawData()));

    }

    @Test
    public void canReplaceAltitude() {

        PointRecord<String> pt = new PointRecord.Builder<String>()
            .time(Instant.EPOCH)
            .latLong(0.0, 1.1)
            .payload("hello")
            .build();

        PointRecord<String> pt2 = pt.withAltitude(Distance.ofFeet(123));

        assertThat(pt2.latLong(), is(pt.latLong()));
        assertThat(pt2.time(), is(pt.time()));
        assertThat(pt2.linkId(), is(pt.linkId()));
        assertThat(pt2.altitude(), is(not(pt.altitude())));
        assertThat(pt2.altitude(), is(Distance.ofFeet(123)));
        assertThat(pt2.rawData(), is(pt.rawData()));
    }


    @Test
    public void canReplaceLatLong() {

        PointRecord<String> pt = new PointRecord.Builder<String>()
            .time(Instant.EPOCH)
            .latLong(0.0, 1.1)
            .payload("hello")
            .build();

        PointRecord<String> pt2 = pt.withLatLong(LatLong.of(31.0, 24.5));

        assertThat(pt2.latLong(), is(not(pt.latLong())));
        assertThat(pt2.latLong(), is(LatLong.of(31.0, 24.5)));
        assertThat(pt2.time(), is(pt.time()));
        assertThat(pt2.linkId(), is(pt.linkId()));
        assertThat(pt2.altitude(), is(pt.altitude()));
        assertThat(pt2.rawData(), is(pt.rawData()));
    }


}