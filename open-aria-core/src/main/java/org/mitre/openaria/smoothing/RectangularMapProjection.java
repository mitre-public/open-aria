
package org.mitre.openaria.smoothing;

import org.mitre.openaria.core.Point;
import org.mitre.caasd.commons.Pair;
import org.mitre.caasd.commons.math.Vector;

/**
 * Very basic projection from lat/lon to X/Y intended to be fast by providing scaled coordinates at
 * the origin
 */
public class RectangularMapProjection {

    public static final double NM_PER_DEGREE_LAT = 60.0068669107676;
    private final double nmPerDegreeLon;
    private final Double lat0;
    private final Double lon0;

    public RectangularMapProjection(Double lat0, Double lon0) {
        this.lat0 = lat0;
        this.lon0 = lon0;
        nmPerDegreeLon = NM_PER_DEGREE_LAT * Math.cos(Math.PI / 180.0 * lat0);
    }

    public RectangularMapProjection(Point pt) {
        this(pt.latLong().latitude(), pt.latLong().longitude());
    }

    public Pair<Double, Double> relativeCoordinates(Point pt) {
        return relativeCoordinates(pt.latLong().latitude(), pt.latLong().longitude());
    }

    public Double yCoordinate(Double latitude) {
        return NM_PER_DEGREE_LAT * (latitude - lat0);
    }

    public Double xCoordinate(Double longitude) {
        return nmPerDegreeLon * (longitude - lon0);
    }

    public Double yCoordinate(Point point) {
        return yCoordinate(point.latLong().latitude());
    }

    public Double xCoordinate(Point point) {
        return xCoordinate(point.latLong().longitude());
    }

    public Pair<Double, Double> relativeCoordinates(Double latitude, Double longitude) {
        return new Pair<>(xCoordinate(longitude), yCoordinate(latitude));
    }

    public Pair<Double, Double> globalCoordinates(Double x, Double y) {
        double lat = lat0 + (y / NM_PER_DEGREE_LAT);
        double lon = lon0 + (x / nmPerDegreeLon);
        return new Pair<>(lat, lon);
    }

    public Vector coordinateVector(Point point) {
        return Vector.of(xCoordinate(point), yCoordinate(point));
    }
}
