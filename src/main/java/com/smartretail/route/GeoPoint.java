package com.smartretail.route;

/** A point on the map in decimal degrees. */
public record GeoPoint(double lat, double lng) {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public GeoPoint {
        if (!Double.isFinite(lat) || lat < -90 || lat > 90 || !Double.isFinite(lng) || lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Not a valid map point: " + lat + ", " + lng);
        }
    }

    /** Great-circle (haversine) distance in kilometres. */
    public double distanceKmTo(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - lat);
        double dLng = Math.toRadians(other.lng - lng);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
