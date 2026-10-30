package com.smartretail.route;

import java.util.List;

/**
 * The area a delivery route covers. A new kind of area is a new implementation; the code that
 * places shops on routes only asks {@link #contains}.
 */
public sealed interface DeliveryZone permits DeliveryZone.Polygon, DeliveryZone.Circle {

    boolean contains(GeoPoint point);

    /** An area drawn point by point. Areas are city-sized, so treating degrees as flat is accurate enough. */
    record Polygon(List<GeoPoint> vertices) implements DeliveryZone {

        public static final int MIN_POINTS = 3;
        public static final int MAX_POINTS = 200;

        public Polygon {
            if (vertices == null || vertices.size() < MIN_POINTS || vertices.size() > MAX_POINTS) {
                throw new IllegalArgumentException(
                        "An area needs " + MIN_POINTS + " to " + MAX_POINTS + " points");
            }
            vertices = List.copyOf(vertices);
        }

        /** Ray casting: a point is inside when a ray from it crosses the edges an odd number of times. */
        @Override
        public boolean contains(GeoPoint point) {
            boolean inside = false;
            for (int i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
                GeoPoint a = vertices.get(i);
                GeoPoint b = vertices.get(j);
                boolean crosses = (a.lat() > point.lat()) != (b.lat() > point.lat())
                        && point.lng() < (b.lng() - a.lng()) * (point.lat() - a.lat()) / (b.lat() - a.lat()) + a.lng();
                if (crosses) {
                    inside = !inside;
                }
            }
            return inside;
        }
    }

    record Circle(GeoPoint center, double radiusKm) implements DeliveryZone {

        public static final double MIN_RADIUS_KM = 0.1;
        public static final double MAX_RADIUS_KM = 100;

        public Circle {
            if (center == null) {
                throw new IllegalArgumentException("A circle needs a centre");
            }
            if (!(radiusKm >= MIN_RADIUS_KM && radiusKm <= MAX_RADIUS_KM)) {
                throw new IllegalArgumentException(
                        "The radius must be " + MIN_RADIUS_KM + " to " + (int) MAX_RADIUS_KM + " km");
            }
        }

        @Override
        public boolean contains(GeoPoint point) {
            return center.distanceKmTo(point) <= radiusKm;
        }
    }
}
