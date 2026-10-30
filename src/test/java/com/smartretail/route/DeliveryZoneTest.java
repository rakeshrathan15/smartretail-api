package com.smartretail.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DeliveryZoneTest {

    // Roughly Ameerpet–Begumpet–Somajiguda–Panjagutta in Hyderabad.
    private static final DeliveryZone.Polygon AREA = new DeliveryZone.Polygon(List.of(
            new GeoPoint(17.4400, 78.4400),
            new GeoPoint(17.4400, 78.4700),
            new GeoPoint(17.4200, 78.4700),
            new GeoPoint(17.4200, 78.4400)));

    @Test
    void polygonContainsPointsInsideOnly() {
        assertThat(AREA.contains(new GeoPoint(17.4300, 78.4500))).isTrue();
        assertThat(AREA.contains(new GeoPoint(17.4500, 78.4500))).isFalse();
        assertThat(AREA.contains(new GeoPoint(17.4300, 78.4800))).isFalse();
    }

    @Test
    void concavePolygonExcludesItsNotch() {
        // An L shape: the top-right square is cut out.
        DeliveryZone.Polygon shape = new DeliveryZone.Polygon(List.of(
                new GeoPoint(17.40, 78.40), new GeoPoint(17.40, 78.50), new GeoPoint(17.45, 78.50),
                new GeoPoint(17.45, 78.45), new GeoPoint(17.50, 78.45), new GeoPoint(17.50, 78.40)));
        assertThat(shape.contains(new GeoPoint(17.42, 78.48))).isTrue();
        assertThat(shape.contains(new GeoPoint(17.48, 78.42))).isTrue();
        assertThat(shape.contains(new GeoPoint(17.48, 78.48))).isFalse();
    }

    @Test
    void circleUsesRealDistance() {
        GeoPoint charminar = new GeoPoint(17.3616, 78.4747);
        DeliveryZone.Circle circle = new DeliveryZone.Circle(charminar, 2);
        // About 1.5 km north, and about 2.6 km north.
        assertThat(circle.contains(new GeoPoint(17.3751, 78.4747))).isTrue();
        assertThat(circle.contains(new GeoPoint(17.3850, 78.4747))).isFalse();
        assertThat(charminar.distanceKmTo(new GeoPoint(17.3850, 78.4747))).isBetween(2.55, 2.65);
    }

    @Test
    void rejectsShapesThatCannotBeAnArea() {
        assertThatThrownBy(() -> new DeliveryZone.Polygon(List.of(new GeoPoint(17.4, 78.4), new GeoPoint(17.5, 78.5))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("3 to 200 points");
        assertThatThrownBy(() -> new DeliveryZone.Circle(new GeoPoint(17.4, 78.4), 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("radius");
        assertThatThrownBy(() -> new DeliveryZone.Circle(new GeoPoint(17.4, 78.4), Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(95, 78.4)).isInstanceOf(IllegalArgumentException.class);
    }
}
