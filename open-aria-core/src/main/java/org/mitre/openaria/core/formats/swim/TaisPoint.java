package org.mitre.openaria.core.formats.swim;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

import org.mitre.caasd.commons.Spherical;




public final class TaisPoint {

    public final String tracon;

    /**
     * Timestamp provided by the Multi Radar Tracker.
     */
    public final Long time;
    public final Double xpos;
    public final Double ypos;

    // swim 4.0
    @Nullable
    public final String eram_gufi;
    @Nullable public final String sfdps_gufi;

    @Nullable
    public final String ac_address;
    @Nullable
    public final Short adsb;
    @Nullable
    public final Short frozen;

    @Nullable
    public final Short new_prop;
    @Nullable
    public final Short pseudo;
    @Nullable
    public final Double reported_altitude;
    @Nullable
    public final String reported_beacon_code;
    @Nullable
    public final String status;
    @Nullable
    public final Integer track_num;
    @Nullable
    public final Double vvert;
    @Nullable
    public final Double vx;
    @Nullable
    public final Double vy;

    public final Double latitude;

    public final Double longitude;
    @Nullable
    public final String arr_airport;
    @Nullable
    public final String dep_airport;
    @Nullable
    public final String scratchpad1;
    @Nullable
    public final String scratchpad2;
    @Nullable
    public final String callsign;
    @Nullable
    public final String aircraft_type;
    @Nullable
    public final String flight_rules;
    @Nullable
    public final String keyboard;
    @Nullable
    public final String position_symbol;

    private TaisPoint() {
        this.tracon = null;
        this.time = null;
        this.xpos = null;
        this.ypos = null;
        this.eram_gufi = null;
        this.sfdps_gufi = null;
        this.ac_address = null;
        this.adsb = null;
        this.frozen = null;
        this.new_prop = null;
        this.pseudo = null;
        this.reported_altitude = null;
        this.reported_beacon_code = null;
        this.status = null;
        this.track_num = null;
        this.vvert = null;
        this.vx = null;
        this.vy = null;
        this.latitude = null;
        this.longitude = null;
        this.arr_airport = null;
        this.dep_airport = null;
        this.scratchpad1 = null;
        this.scratchpad2 = null;
        this.callsign = null;
        this.aircraft_type = null;
        this.flight_rules = null;
        this.keyboard = null;
        this.position_symbol = null;
    }

    private TaisPoint(Builder builder) {
        this.tracon = requireNonNull(builder.tracon);
        this.time = requireNonNull(builder.time);
        this.xpos = requireNonNull(builder.xpos);
        this.ypos = requireNonNull(builder.ypos);
        this.eram_gufi = builder.eramGufi;
        this.sfdps_gufi = builder.sfdpsGufi;
        this.ac_address = builder.acAddress;
        this.adsb = builder.adsb;
        this.frozen = builder.frozen;
        this.new_prop = builder.newProp;
        this.pseudo = builder.pseudo;
        this.reported_altitude = builder.reportedAltitude;
        this.reported_beacon_code = builder.reportedBeaconCode;
        this.status = builder.status;
        this.track_num = builder.trackNum;
        this.vvert = builder.vvert;
        this.vx = builder.vx;
        this.vy = builder.vy;
        this.latitude = requireNonNull(builder.latitude);
        this.longitude = requireNonNull(builder.longitude);
        this.arr_airport = builder.arrAirport;
        this.dep_airport = builder.depAirport;
        this.scratchpad1 = builder.scratchpad1;
        this.scratchpad2 = builder.scratchpad2;
        this.callsign = builder.callsign;
        this.aircraft_type = builder.aircraftType;
        this.flight_rules = builder.flightRules;
        this.keyboard = builder.keyboard;
        this.position_symbol = builder.positionSymbol;
    }

    public String tracon() {
        return tracon;
    }

    public Long getTime() {
        return time;
    }

    public Double xpos() {
        return xpos;
    }

    public Double ypos() {
        return ypos;
    }

    public String eramGufi() {
        return eram_gufi;
    }

    public String sfdpsGufi() {
        return sfdps_gufi;
    }

    public String acAddress() {
        return ac_address;
    }

    public Short adsb() {
        return adsb;
    }

    public Short frozen() {
        return frozen;
    }

    public Short newProp() {
        return new_prop;
    }

    public Short pseudo() {
        return pseudo;
    }

    public Double reportedAltitude() {
        return reported_altitude;
    }

    public String reportedBeaconCode() {
        return reported_beacon_code;
    }

    public Optional<TrackStatusType> status() {
        return Optional.ofNullable(status).map(TrackStatusType::valueOf);
    }

    public Integer trackNum() {
        return track_num;
    }

    public Double vvert() {
        return vvert;
    }

    public Double vx() {
        return vx;
    }

    public Double vy() {
        return vy;
    }

    public Double lat() {
        return latitude;
    }

    public Double lon() {
        return longitude;
    }

    public String arrAirport() {
        return arr_airport;
    }

    public String depAirport() {
        return dep_airport;
    }

    public String scratchpad1() {
        return scratchpad1;
    }

    public String scratchpad2() {
        return scratchpad2;
    }

    public String callsign() {
        return callsign;
    }

    public String aircraftType() {
        return aircraft_type;
    }

    public String flightRules() {
        return flight_rules;
    }

    public String keyboard() {
        return keyboard;
    }

    public String positionSymbol() {
        return position_symbol;
    }

    public Builder toBuilder() {
        return new Builder()
            .tracon(tracon())
            .time(getTime())
            .xpos(xpos())
            .ypos(ypos())
            .eramGufi(eramGufi())
            .sfdpsGufi(sfdpsGufi())
            .acAddress(acAddress())
            .adsb(adsb())
            .frozen(frozen())
            .newProp(newProp())
            .pseudo(pseudo())
            .reportedAltitude(reportedAltitude())
            .reportedBeaconCode(reportedBeaconCode())
            .status(status().map(Object::toString).orElse(null))
            .trackNum(trackNum())
            .vvert(vvert())
            .vx(vx())
            .vy(vy())
            .latitude(lat())
            .longitude(lon())
            .arrAirport(arrAirport())
            .depAirport(depAirport())
            .scratchpad1(scratchpad1())
            .scratchpad2(scratchpad2())
            .callsign(callsign())
            .aircraftType(aircraftType())
            .flightRules(flightRules())
            .keyboard(keyboard())
            .positionSymbol(positionSymbol());
    }

    // TDP-Required interface methods for point records

    public Double getTrueCourseInDegrees() {
        return (vx == null || vy == null) ? null : Spherical.mod(Math.toDegrees(Math.atan2(vx, vy)), 360.0);
    }

    public Double getSpeedInKnots() {
        return (vx == null || vy == null) ? null : Math.sqrt(vx * vx + vy * vy);
    }

    public Instant time() {
        return Instant.ofEpochMilli(time);
    }

    public Double getLatitudeInDegrees() {
        return latitude;
    }

    public Double getLongitudeInDegrees() {
        return longitude;
    }


    public Double getAltitude() {
        return reported_altitude;
    }


    public String getFacility() {
        return tracon;
    }

    public String getTrackNumber() {
        return track_num == null ? null : track_num.toString();
    }

//    @Override
//    public Source getSource() {
//        return Source.TAIS;
//    }


    public String getCallsign() {
        return callsign;
    }


    public String getReportedBeacon() {
        return reported_beacon_code;
    }

//
//    @Override
//    public String getMeta(Attribute key) {
//        switch (key) {
//            case ARR_AIRPORT:
//                return arr_airport;
//            case DEP_AIRPORT:
//                return dep_airport;
//            case SCRATCHPAD1:
//                return scratchpad1;
//            case SCRATCHPAD2:
//                return scratchpad2;
//            case TRACK_NUMBER:
//                return track_num.toString();
//            case BEACON_CODE:
//                return reported_beacon_code;
//            case CALLSIGN:
//                return callsign;
//            case AIRCRAFT_TYPE:
//                return aircraft_type;
//            case FLIGHT_RULES:
//                return flight_rules;
//            case GUFI:
//                return eram_gufi;
//            case KEYBOARD:
//                return keyboard;
//            case POSITION_SYMBOL:
//                return position_symbol;
//            default:
//                return null;
//        }
//    }

//    @Override
//    public Map<String, String> getSourceId() {
//        HashMap<String, String> sourceId = new HashMap<>();
//        sourceId.put(Tracon.class.getName(), tracon);
//        return sourceId;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaisPoint taisPoint = (TaisPoint) o;
        return Objects.equals(tracon, taisPoint.tracon)
            && Objects.equals(time, taisPoint.time)
            && Objects.equals(xpos, taisPoint.xpos)
            && Objects.equals(ypos, taisPoint.ypos)
            && Objects.equals(eram_gufi, taisPoint.eram_gufi)
            && Objects.equals(sfdps_gufi, taisPoint.sfdps_gufi)
            && Objects.equals(ac_address, taisPoint.ac_address)
            && Objects.equals(adsb, taisPoint.adsb)
            && Objects.equals(frozen, taisPoint.frozen)
            && Objects.equals(new_prop, taisPoint.new_prop)
            && Objects.equals(pseudo, taisPoint.pseudo)
            && Objects.equals(reported_altitude, taisPoint.reported_altitude)
            && Objects.equals(reported_beacon_code, taisPoint.reported_beacon_code)
            && Objects.equals(status, taisPoint.status)
            && Objects.equals(track_num, taisPoint.track_num)
            && Objects.equals(vvert, taisPoint.vvert)
            && Objects.equals(vx, taisPoint.vx)
            && Objects.equals(vy, taisPoint.vy)
            && Objects.equals(latitude, taisPoint.latitude)
            && Objects.equals(longitude, taisPoint.longitude)
            && Objects.equals(arr_airport, taisPoint.arr_airport)
            && Objects.equals(dep_airport, taisPoint.dep_airport)
            && Objects.equals(scratchpad1, taisPoint.scratchpad1)
            && Objects.equals(scratchpad2, taisPoint.scratchpad2)
            && Objects.equals(callsign, taisPoint.callsign)
            && Objects.equals(aircraft_type, taisPoint.aircraft_type)
            && Objects.equals(flight_rules, taisPoint.flight_rules)
            && Objects.equals(keyboard, taisPoint.keyboard)
            && Objects.equals(position_symbol, taisPoint.position_symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tracon, time, xpos, ypos, eram_gufi, sfdps_gufi, ac_address, adsb, frozen, new_prop, pseudo, reported_altitude, reported_beacon_code, status, track_num, vvert, vx, vy, latitude, longitude, arr_airport, dep_airport, scratchpad1, scratchpad2, callsign, aircraft_type, flight_rules, keyboard, position_symbol);
    }

    @Override
    public String toString() {
        return "TaisPoint{"
            + "tracon='" + tracon + '\''
            + ", time=" + time
            + ", xpos=" + xpos
            + ", ypos=" + ypos
            + ", eram_gufi='" + eram_gufi + '\''
            + ", sfdps_gufi='" + sfdps_gufi + '\''
            + ", ac_address='" + ac_address + '\''
            + ", adsb=" + adsb
            + ", frozen=" + frozen
            + ", new_prop=" + new_prop
            + ", pseudo=" + pseudo
            + ", reported_altitude=" + reported_altitude
            + ", reported_beacon_code='" + reported_beacon_code + '\''
            + ", status='" + status + '\''
            + ", track_num=" + track_num
            + ", vvert=" + vvert
            + ", vx=" + vx
            + ", vy=" + vy
            + ", lat=" + latitude
            + ", lon=" + longitude
            + ", arr_airport='" + arr_airport + '\''
            + ", dep_airport='" + dep_airport + '\''
            + ", scratchpad1='" + scratchpad1 + '\''
            + ", scratchpad2='" + scratchpad2 + '\''
            + ", callsign='" + callsign + '\''
            + ", aircraft_type='" + aircraft_type + '\''
            + ", flight_rules='" + flight_rules + '\''
            + ", keyboard='" + keyboard + '\''
            + ", position_symbol='" + position_symbol + '\''
            + '}';
    }

    public enum TrackStatusType {
        ACTIVE,
        COASTING,
        DROP
    }

    public static final class Builder {
        private String tracon;
        private Long time;
        private Double xpos;
        private Double ypos;
        private String eramGufi;
        private String sfdpsGufi;
        private String acAddress;
        private Short adsb;
        private Short frozen;
        private Short newProp;
        private Short pseudo;
        private Double reportedAltitude;
        private String reportedBeaconCode;
        private String status;
        private Integer trackNum;
        private Double vvert;
        private Double vx;
        private Double vy;
        private Double latitude;
        private Double longitude;
        private String arrAirport;
        private String depAirport;
        private String scratchpad1;
        private String scratchpad2;
        private String callsign;
        private String aircraftType;
        private String flightRules;
        private String keyboard;
        private String positionSymbol;

        public Builder tracon(String tracon) {
            this.tracon = tracon;
            return this;
        }

        public Builder time(Long time) {
            this.time = time;
            return this;
        }

        public Builder xpos(Double xpos) {
            this.xpos = xpos;
            return this;
        }

        public Builder ypos(Double ypos) {
            this.ypos = ypos;
            return this;
        }

        public Builder eramGufi(String eramGufi) {
            this.eramGufi = eramGufi;
            return this;
        }

        public Builder sfdpsGufi(String sfdpsGufi) {
            this.sfdpsGufi = sfdpsGufi;
            return this;
        }

        public Builder acAddress(String acAddress) {
            this.acAddress = acAddress;
            return this;
        }

        public Builder adsb(Short adsb) {
            this.adsb = adsb;
            return this;
        }

        public Builder frozen(Short frozen) {
            this.frozen = frozen;
            return this;
        }

        public Builder newProp(Short newProp) {
            this.newProp = newProp;
            return this;
        }

        public Builder pseudo(Short pseudo) {
            this.pseudo = pseudo;
            return this;
        }

        public Builder reportedAltitude(Double reportedAltitude) {
            this.reportedAltitude = reportedAltitude;
            return this;
        }

        public Builder reportedBeaconCode(String reportedBeaconCode) {
            this.reportedBeaconCode = reportedBeaconCode;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder trackNum(Integer trackNum) {
            this.trackNum = trackNum;
            return this;
        }

        public Builder vvert(Double vvert) {
            this.vvert = vvert;
            return this;
        }

        public Builder vx(Double vx) {
            this.vx = vx;
            return this;
        }

        public Builder vy(Double vy) {
            this.vy = vy;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder arrAirport(String arrAirport) {
            this.arrAirport = arrAirport;
            return this;
        }

        public Builder depAirport(String depAirport) {
            this.depAirport = depAirport;
            return this;
        }

        public Builder scratchpad1(String scratchpad1) {
            this.scratchpad1 = scratchpad1;
            return this;
        }

        public Builder scratchpad2(String scratchpad2) {
            this.scratchpad2 = scratchpad2;
            return this;
        }

        public Builder callsign(String callsign) {
            this.callsign = callsign;
            return this;
        }

        public Builder aircraftType(String aircraftType) {
            this.aircraftType = aircraftType;
            return this;
        }

        public Builder flightRules(String flightRules) {
            this.flightRules = flightRules;
            return this;
        }

        public Builder keyboard(String keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        public Builder positionSymbol(String positionSymbol) {
            this.positionSymbol = positionSymbol;
            return this;
        }

        public TaisPoint build() {
            return new TaisPoint(this);
        }
    }
}
