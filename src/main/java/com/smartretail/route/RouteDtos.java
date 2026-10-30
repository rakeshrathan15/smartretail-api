package com.smartretail.route;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class RouteDtos {

    private RouteDtos() {
    }

    /** Routes are local delivery runs, so their points must be in India (catches swapped lat/lng). */
    public record Point(
            @NotNull @DecimalMin("6.0") @DecimalMax("37.5") Double lat,
            @NotNull @DecimalMin("68.0") @DecimalMax("98.0") Double lng) {

        static Point from(GeoPoint point) {
            return new Point(point.lat(), point.lng());
        }

        GeoPoint toGeoPoint() {
            return new GeoPoint(lat, lng);
        }
    }

    public enum ZoneShape {
        POLYGON,
        CIRCLE
    }

    /** A drawn area: POLYGON uses {@code points}; CIRCLE uses {@code center} and {@code radiusKm}. */
    public record Zone(
            @NotNull ZoneShape shape,
            @Size(max = DeliveryZone.Polygon.MAX_POINTS) List<@Valid @NotNull Point> points,
            @Valid Point center,
            Double radiusKm) {

        static Zone from(DeliveryZone zone) {
            return switch (zone) {
                case DeliveryZone.Polygon polygon -> new Zone(ZoneShape.POLYGON,
                        polygon.vertices().stream().map(Point::from).toList(), null, null);
                case DeliveryZone.Circle circle -> new Zone(ZoneShape.CIRCLE, null, Point.from(circle.center()),
                        circle.radiusKm());
            };
        }
    }

    public record RouteRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 120) String area,
            @NotNull @Size(max = 7) List<@NotNull @Pattern(regexp = "MON|TUE|WED|THU|FRI|SAT|SUN",
                    message = "must be MON, TUE, WED, THU, FRI, SAT or SUN") String> days,
            UUID storeId,
            @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "must be a colour like #2563EB") String color,
            @Valid Zone zone,
            @Min(0) @Max(9999) Integer sortOrder,
            Boolean active) {
    }

    public record ActiveRequest(boolean active) {
    }

    public record RouteResponse(
            UUID id, String name, String area, List<String> days, UUID storeId, String storeName, String color,
            Zone zone, boolean active, int sortOrder, long shopCount, LocalDate nextDeliveryDate) {
    }

    public record ShopPinResponse(UUID userId, String shopName, double lat, double lng, UUID routeId) {
    }

    public record ReassignResponse(int assigned, int unassigned) {
    }

    /** The signed-in shop's route, for the app and website. */
    public record MyRouteResponse(
            boolean assigned, String name, String area, List<String> days, LocalDate nextDeliveryDate) {

        static final MyRouteResponse NONE = new MyRouteResponse(false, null, null, List.of(), null);
    }
}
