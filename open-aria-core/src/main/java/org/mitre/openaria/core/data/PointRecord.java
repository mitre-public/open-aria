package org.mitre.openaria.core.data;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;

/**
 * A PointRecord is a basic implementation of the Point interface
 *
 * @param time     The time at which a location measurement is taken
 * @param linkId   A unique string that can be used to "link" multiple Points together
 * @param latLong  The location
 * @param altitude The altitude
 * @param payload  Additional data "above and beyond" the simple position data covered by Point
 * @param <T>      The "type" of the payload field (byte[], String, Domain specific POJO, etc.)
 */
public record PointRecord<T>(Instant time, String linkId, LatLong latLong, Distance altitude,
                             T payload) implements Point<T> {

    @Override
    public T rawData() {
        return null;
    }

    public PointRecord<T> withTime(Instant replacementTime) {
        return new Builder<>(this).time(replacementTime).build();
    }

    public PointRecord<T> withLatLong(LatLong replaceLoc) {
        return new Builder<>(this).latLong(replaceLoc).build();
    }

    public PointRecord<T> withAltitude(Distance replacementAltitude) {
        return new Builder<>(this).altitude(replacementAltitude).build();
    }

    public static Builder<?> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {

        Instant time;

        String linkId;

        LatLong latLong;

        Distance altitude;

        T payload;

        public Builder() {
        }

        /**
         * Copy all the attributes of the input Point p
         *
         * @param p A Point
         */
        public Builder(Point<T> p) {
            this();
            this.time = p.time();
            this.linkId = p.linkId();
            this.latLong = p.latLong();
            this.altitude = p.altitude();
            this.payload = p.rawData();
        }

        public Builder<T> time(Instant time) {
            requireNonNull(time);
            this.time = time;
            return this;
        }

        public Builder<T> linkId(String linkId) {
            this.linkId = linkId;
            return this;
        }

        public Builder<T> latLong(LatLong latitudeAndLongitude) {
            requireNonNull(latitudeAndLongitude);
            this.latLong = latitudeAndLongitude;
            return this;
        }

        public Builder<T> latLong(Double latitude, Double longitude) {
            requireNonNull(latitude);
            requireNonNull(longitude);
            this.latLong = LatLong.of(latitude, longitude);
            return this;
        }

        public Builder<T> altitude(Distance altitude) {
            this.altitude = altitude;
            return this;
        }

        public Builder<T> altitudeInFeet(double alt) {
            this.altitude = Distance.ofFeet(alt);
            return this;
        }

        public Builder<T> payload(T payload) {
            this.payload = payload;
            return this;
        }

        public PointRecord<T> build() {
            return new PointRecord<T>(time, linkId, latLong, altitude, payload);
        }
    }
}
