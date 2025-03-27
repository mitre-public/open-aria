package org.mitre.openaria.core.formats.swim;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;


public final class TaisFlightplan {

    @Nullable
    public final String acid;
    @Nullable public final String ac_type;
    @Nullable final String assigned_beacon_code;
    @Nullable public final String airport;
    @Nullable public final String category;
    @Nullable public final String dbi;
    @Nullable public final String ecid;
    @Nullable public final String entry_fix;
    @Nullable public final String exit_fix;
    @Nullable public final String flight_rules;
    @Nullable public final String lld;
    @Nullable public final String ocr;
    @Nullable public final String ptd_time;
    @Nullable public final Long requested_altitude;
    @Nullable public final Short rnav;
    @Nullable public final String runway;
    @Nullable public final String scratch_pad_1;
    @Nullable public final String scratch_pad_2;
    @Nullable public final Integer sfpn;
    @Nullable public final String status;
    @Nullable public final Short suspended;
    @Nullable public final String type;
    @Nullable public final String keyboard;
    @Nullable public final String position_symbol;

    private TaisFlightplan() {
        this.acid = null;
        this.ac_type = null;
        this.assigned_beacon_code = null;
        this.airport = null;
        this.category = null;
        this.dbi = null;
        this.ecid = null;
        this.entry_fix = null;
        this.exit_fix = null;
        this.flight_rules = null;
        this.lld = null;
        this.ocr = null;
        this.ptd_time = null;
        this.requested_altitude = null;
        this.rnav = null;
        this.runway = null;
        this.scratch_pad_1 = null;
        this.scratch_pad_2 = null;
        this.sfpn = null;
        this.status = null;
        this.suspended = null;
        this.type = null;
        this.keyboard = null;
        this.position_symbol = null;
    }

    private TaisFlightplan(Builder builder) {
        this.acid = builder.acid;
        this.ac_type = builder.acType;
        this.assigned_beacon_code = builder.assignedBeaconCode;
        this.airport = builder.airport;
        this.category = builder.category;
        this.dbi = builder.dbi;
        this.ecid = builder.ecid;
        this.entry_fix = builder.entryFix;
        this.exit_fix = builder.exitFix;
        this.flight_rules = builder.flightRules;
        this.lld = builder.lld;
        this.ocr = builder.ocr;
        this.ptd_time = builder.ptdTime;
        this.requested_altitude = builder.requestedAltitude;
        this.rnav = builder.rnav;
        this.runway = builder.runway;
        this.scratch_pad_1 = builder.scratchPad1;
        this.scratch_pad_2 = builder.scratchPad2;
        this.sfpn = builder.sfpn;
        this.status = builder.status;
        this.suspended = builder.suspended;
        this.type = builder.type;
        this.keyboard = builder.keyboard;
        this.position_symbol = builder.positionSymbol;
    }

    public String acid() {
        return acid;
    }

    public String acType() {
        return ac_type;
    }

    public String assignedBeaconCode() {
        return assigned_beacon_code;
    }

    public String airport() {
        return airport;
    }

    public String category() {
        return category;
    }

    public String dbi() {
        return dbi;
    }

    public String ecid() {
        return ecid;
    }

    public String entryFix() {
        return entry_fix;
    }

    public String exitFix() {
        return exit_fix;
    }

    public String flightRules() {
        return flight_rules;
    }

    public String lld() {
        return lld;
    }

    public String ocr() {
        return ocr;
    }

    public String ptdTime() {
        return ptd_time;
    }

    public Long requestedAltitude() {
        return requested_altitude;
    }

    public Short rnav() {
        return rnav;
    }

    public String runway() {
        return runway;
    }

    public String scratchPad1() {
        return scratch_pad_1;
    }

    public String scratchPad2() {
        return scratch_pad_2;
    }

    public Integer sfpn() {
        return sfpn;
    }

    public String status() {
        return status;
    }

    public Short suspended() {
        return suspended;
    }

    public Optional<TypeofFlightType> type() {
        return Optional.ofNullable(type).map(TypeofFlightType::valueOf);
    }

    public String keyboard() {
        return keyboard;
    }

    public String positionSymbol() {
        return position_symbol;
    }

    public Builder toBuilder() {
        return new Builder()
            .acid(acid())
            .acType(acType())
            .assignedBeaconCode(assignedBeaconCode())
            .airport(airport())
            .category(category())
            .dbi(dbi())
            .ecid(ecid())
            .entryFix(entryFix())
            .exitFix(exitFix())
            .flightRules(flightRules())
            .lld(lld())
            .ocr(ocr())
            .ptdTime(ptdTime())
            .requestedAltitude(requestedAltitude())
            .rnav(rnav())
            .runway(runway())
            .scratchPad1(scratchPad1())
            .scratchPad2(scratchPad2())
            .sfpn(sfpn())
            .status(status())
            .suspended(suspended())
            .type(type().map(TypeofFlightType::name).orElse(null))
            .keyboard(keyboard())
            .positionSymbol(positionSymbol());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaisFlightplan that = (TaisFlightplan) o;
        return Objects.equals(acid, that.acid)
            && Objects.equals(ac_type, that.ac_type)
            && Objects.equals(assigned_beacon_code, that.assigned_beacon_code)
            && Objects.equals(airport, that.airport)
            && Objects.equals(category, that.category)
            && Objects.equals(dbi, that.dbi)
            && Objects.equals(ecid, that.ecid)
            && Objects.equals(entry_fix, that.entry_fix)
            && Objects.equals(exit_fix, that.exit_fix)
            && Objects.equals(flight_rules, that.flight_rules)
            && Objects.equals(lld, that.lld)
            && Objects.equals(ocr, that.ocr)
            && Objects.equals(ptd_time, that.ptd_time)
            && Objects.equals(requested_altitude, that.requested_altitude)
            && Objects.equals(rnav, that.rnav)
            && Objects.equals(runway, that.runway)
            && Objects.equals(scratch_pad_1, that.scratch_pad_1)
            && Objects.equals(scratch_pad_2, that.scratch_pad_2)
            && Objects.equals(sfpn, that.sfpn)
            && Objects.equals(status, that.status)
            && Objects.equals(suspended, that.suspended)
            && Objects.equals(type, that.type)
            && Objects.equals(keyboard, that.keyboard)
            && Objects.equals(position_symbol, that.position_symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(acid, ac_type, assigned_beacon_code, airport, category, dbi, ecid, entry_fix, exit_fix, flight_rules, lld, ocr, ptd_time, requested_altitude, rnav, runway, scratch_pad_1, scratch_pad_2, sfpn, status, suspended, type, keyboard, position_symbol);
    }

    public static final class Builder {
        private String acid;
        private String acType;
        private String assignedBeaconCode;
        private String airport;
        private String category;
        private String dbi;
        private String ecid;
        private String entryFix;
        private String exitFix;
        private String flightRules;
        private String lld;
        private String ocr;
        private String ptdTime;
        private Long requestedAltitude;
        private Short rnav;
        private String runway;
        private String scratchPad1;
        private String scratchPad2;
        private Integer sfpn;
        private String status;
        private Short suspended;
        private String type;
        private String keyboard;
        private String positionSymbol;

        public Builder acid(String acid) {
            this.acid = acid;
            return this;
        }

        public Builder acType(String acType) {
            this.acType = acType;
            return this;
        }

        public Builder assignedBeaconCode(String assignedBeaconCode) {
            this.assignedBeaconCode = assignedBeaconCode;
            return this;
        }

        public Builder airport(String airport) {
            this.airport = airport;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder dbi(String dbi) {
            this.dbi = dbi;
            return this;
        }

        public Builder ecid(String ecid) {
            this.ecid = ecid;
            return this;
        }

        public Builder entryFix(String entryFix) {
            this.entryFix = entryFix;
            return this;
        }

        public Builder exitFix(String exitFix) {
            this.exitFix = exitFix;
            return this;
        }

        public Builder flightRules(String flightRules) {
            this.flightRules = flightRules;
            return this;
        }

        public Builder lld(String lld) {
            this.lld = lld;
            return this;
        }

        public Builder ocr(String ocr) {
            this.ocr = ocr;
            return this;
        }

        public Builder ptdTime(String ptdTime) {
            this.ptdTime = ptdTime;
            return this;
        }

        public Builder requestedAltitude(Long requestedAltitude) {
            this.requestedAltitude = requestedAltitude;
            return this;
        }

        public Builder rnav(Short rnav) {
            this.rnav = rnav;
            return this;
        }

        public Builder runway(String runway) {
            this.runway = runway;
            return this;
        }

        public Builder scratchPad1(String scratchPad1) {
            this.scratchPad1 = scratchPad1;
            return this;
        }

        public Builder scratchPad2(String scratchPad2) {
            this.scratchPad2 = scratchPad2;
            return this;
        }

        public Builder sfpn(Integer sfpn) {
            this.sfpn = sfpn;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder suspended(Short suspended) {
            this.suspended = suspended;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
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

        public TaisFlightplan build() {
            return new TaisFlightplan(this);
        }
    }

    public enum FlightRulesType {
        V,
        P,
        E,
        IFR
    }

    public enum LeaderLineDirectionType {
        UNDEFINED,
        NW,
        N,
        NE,
        W,
        E,
        SW,
        S,
        SE
    }

    public enum OwnershipChangeReasonType {
        NO_CHANGE,
        CONSOLIDATION,
        NORMAL_HANDOFF,
        DIRECTED_HANDOFF,
        INTRAFACILITY_HANDOFF,
        MANUAL,
        AUTOMATIC,
        PENDING
    }

    public enum FlightPlanStatusType {
        PENDING,
        ACTIVE,
        TERMINATED,
        PASSIVE
    }

    public enum TypeofFlightType {
        A,
        P,
        D,
        E
    }
}
