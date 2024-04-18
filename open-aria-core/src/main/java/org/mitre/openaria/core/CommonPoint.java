

package org.mitre.openaria.core;

import static com.google.common.base.Preconditions.checkArgument;
import static org.mitre.openaria.core.PointField.CURVATURE;
import static org.mitre.openaria.core.temp.Extras.HasSourceDetails;
import static org.mitre.openaria.core.temp.Extras.SourceDetails;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.mitre.caasd.commons.Distance;
import org.mitre.caasd.commons.LatLong;
import org.mitre.caasd.commons.Spherical;
import org.mitre.caasd.commons.Time;

/**
 * The purpose of CommonPoint is to be a "dependency-free data bridge" between important
 * computations that involve Track Points.
 * <p>
 * Important features of this class are: <br> (1) CommonPoint does not contains (m)any dependencies
 * to external APIs. Therefore, trading CommonPoints between processes leaves each process as free
 * as possible to evolve as independent units.
 * <p>
 * (2) CommonPoint is serializable. Therefore, debugging is dramatically simplified because saving
 * failed inputs to file for future analysis is straight forward and unit tests can be simplified by
 * having "canned input" and
 */
public class CommonPoint<T> implements Point<T>, HasSourceDetails {

    private static final long serialVersionUID = 1L;

    private final String callsign;
    private final String aircraftType;
    private final String trackId;
    private final String beaconActual;
    private final String beaconAssigned;
    private final String flightRules;
    private final LatLong latLong;
    private final Distance altitude;
    private final Double course;
    private final Double speed;
    private final Instant time;
    private final Double curvature;

    private final SourceDetails sourceDetails;
    private final T rawData;


    public CommonPoint(Map<PointField, Object> map, SourceDetails sourceDetails, T rawData) {

        for (Map.Entry<PointField, Object> entry : map.entrySet()) {
            checkArgument(
                entry.getKey().accepts(entry.getValue()),
                "The PointField " + entry.getKey() + " was paired with an illegal value type"
            );
        }

        this.callsign = (String) map.get(PointField.CALLSIGN);
        this.aircraftType = (String) map.get(PointField.AIRCRAFT_TYPE);
        this.trackId = (String) map.get(PointField.TRACK_ID);
        this.beaconActual = (String) map.get(PointField.BEACON_ACTUAL);
        this.beaconAssigned = (String) map.get(PointField.BEACON_ASSIGNED);
        this.flightRules = (String) map.get(PointField.FLIGHT_RULES);
        this.latLong = (LatLong) map.get(PointField.LAT_LONG);
        this.altitude = (Distance) map.get(PointField.ALTITUDE);
        this.course = (Double) map.get(PointField.COURSE_IN_DEGREES);
        this.speed = (Double) map.get(PointField.SPEED);
        this.time = (Instant) map.get(PointField.TIME);
        this.curvature = (Double) map.get(CURVATURE);

        this.sourceDetails = sourceDetails;
        this.rawData = rawData;

        confirmNoEmptyStrings();
    }

    private CommonPoint(CommonPoint<T> copyMe, PointField field, Object newValue) {

        checkArgument(
            field.accepts(newValue),
            "The PointField " + field + " was paired with an illegal value type"
        );

        this.callsign = (field == PointField.CALLSIGN) ? ((String) newValue) : copyMe.callsign;
        this.aircraftType = (field == PointField.AIRCRAFT_TYPE) ? ((String) newValue) : copyMe.aircraftType;
        this.trackId = (field == PointField.TRACK_ID) ? ((String) newValue) : copyMe.trackId;
        this.beaconActual = (field == PointField.BEACON_ACTUAL) ? ((String) newValue) : copyMe.beaconActual;
        this.beaconAssigned = (field == PointField.BEACON_ASSIGNED) ? ((String) newValue) : copyMe.beaconAssigned;
        this.flightRules = (field == PointField.FLIGHT_RULES) ? ((String) newValue) : copyMe.flightRules;
        this.latLong = (field == PointField.LAT_LONG) ? ((LatLong) newValue) : copyMe.latLong;
        this.altitude = (field == PointField.ALTITUDE) ? ((Distance) newValue) : copyMe.altitude;
        this.course = (field == PointField.COURSE_IN_DEGREES) ? ((Double) newValue) : copyMe.course;
        this.speed = (field == PointField.SPEED) ? ((Double) newValue) : copyMe.speed;
        this.time = (field == PointField.TIME) ? ((Instant) newValue) : copyMe.time;
        this.curvature = (field == CURVATURE) ? ((Double) newValue) : copyMe.curvature;

        this.sourceDetails = copyMe.sourceDetails;
        this.rawData = copyMe.rawData;

        confirmNoEmptyStrings();
    }

    private void confirmNoEmptyStrings() {
        /*
         * String fields cannot contain the empty String "". The String "" is too easy to confuse
         * with both a missing value AND a valid value -- Therefore it isn't allowed.
         */
        for (PointField field : PointField.values()) {
            if (field.expectedType == String.class) {
                Object value = field.get(this);
                if ("".equals(value)) {
                    throw new IllegalStateException(
                        "A CommonPoint was built with a String field that equals the empty String."
                            + "  Null is preferable here");
                }
            }
        }
    }

    /**
     * Use two other Points (that we assume are part of the same track) to estimate the course at
     * this Point.
     *
     * @param pointJustBefore The Point "just before" this Point (from the same track)
     * @param pointJustAfter  The Point "just after" this Point (from the same track);
     *
     * @return A Course (in degrees) that was deduced for this point.
     */
    public Double deduceCourse(Point pointJustBefore, Point pointJustAfter) {

        checkBoundaryConditions(pointJustBefore, pointJustAfter);

        Double deducedCourse = Spherical.courseInDegrees(
            pointJustBefore.latLong(),
            pointJustAfter.latLong());

        return deducedCourse;
    }

    /**
     * Use two other Points (that we assume are part of the same track) to estimate the speed at
     * this Point.
     *
     * @param pointJustBefore The Point "just before" this Point (from the same track)
     * @param pointJustAfter  The Point "just after" this Point (from the same track);
     *
     * @return The speed that was deduced for this point.
     */
    public Double deduceSpeed(Point pointJustBefore, Point pointJustAfter) {

        checkBoundaryConditions(pointJustBefore, pointJustAfter);

        Double distance = Spherical.distanceInNM(
            pointJustBefore.latLong(),
            pointJustAfter.latLong()
        );

        Double timeDelta = Time.getDecimalDuration(
            Duration.between(pointJustBefore.time(), pointJustAfter.time()),
            ChronoUnit.HOURS);

        Double deducedSpeed = distance / timeDelta;
        return deducedSpeed;
    }

    private void checkBoundaryConditions(Point pointJustBefore, Point pointJustAfter) {
        checkArgument(
            pointJustAfter.time().isBefore(pointJustAfter.time()),
            "The boundary points must come in chronological order");
        checkArgument(
            pointJustBefore.time().isBefore(this.time) || pointJustBefore.time().equals(this.time),
            "The boundary point \"pointJustBefore\" cannot come occur AFTER this point's time");
        checkArgument(
            this.time.isBefore(pointJustAfter.time()) || this.time.equals(pointJustAfter.time()),
            "The boundary point \"pointJustAfter\" cannot come occur BEFORE this point's time");
    }

    public Point<T> getCopyWithDeducedCourseAndSpeed(Point<T> pointJustBefore, Point<T> pointJustAfter) {

        Map<PointField, Object> map = Points.toMap(this);
        map.put(PointField.SPEED, deduceSpeed(pointJustBefore, pointJustAfter));
        map.put(PointField.COURSE_IN_DEGREES, deduceCourse(pointJustBefore, pointJustAfter));

        return new CommonPoint(map, null, pointJustBefore.rawData());
    }

    @Override
    public String callsign() {
        return this.callsign;
    }

    @Override
    public String aircraftType() {
        return this.aircraftType;
    }

    @Override
    public String trackId() {
        return this.trackId;
    }

    @Override
    public String beaconActual() {
        return this.beaconActual;
    }

    @Override
    public String beaconAssigned() {
        return this.beaconAssigned;
    }

    @Override
    public String flightRules() {
        return this.flightRules;
    }

    @Override
    public Distance altitude() {
        return this.altitude;
    }

    @Override
    public Double course() {
        return this.course;
    }

    @Override
    public Double speedInKnots() {
        return this.speed;
    }

    @Override
    public Instant time() {
        return this.time;
    }

    @Override
    public Double curvature() {
        return this.curvature;
    }

    @Override
    public LatLong latLong() {
        return this.latLong;
    }

    @Override
    public SourceDetails sourceDetails() {
        return this.sourceDetails;
    }

    @Override
    public T rawData() {
        return rawData;
    }
}
