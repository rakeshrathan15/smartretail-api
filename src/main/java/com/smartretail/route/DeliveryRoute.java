package com.smartretail.route;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** A van run: the area it covers, the days it goes and the store it leaves from. */
@Entity
@Table(name = "delivery_routes")
public class DeliveryRoute {

    enum ZoneType {
        POLYGON,
        CIRCLE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String area;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] days = new String[0];

    @Column(name = "store_id")
    private UUID storeId;

    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type")
    private ZoneType zoneType;

    @Convert(converter = GeoPointListConverter.class)
    @Column(name = "zone_polygon", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private List<GeoPoint> zonePolygon;

    @Column(name = "zone_center_lat")
    private BigDecimal zoneCenterLat;

    @Column(name = "zone_center_lng")
    private BigDecimal zoneCenterLng;

    @Column(name = "zone_radius_km")
    private BigDecimal zoneRadiusKm;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected DeliveryRoute() {
    }

    public DeliveryRoute(String name) {
        this.name = name;
    }

    public void update(String name, String area, DeliveryDays days, UUID storeId, String color, int sortOrder) {
        this.name = name;
        this.area = area;
        this.days = days.toCodes();
        this.storeId = storeId;
        this.color = color;
        this.sortOrder = sortOrder;
    }

    /** The area this route covers, or empty when none is drawn yet (no shops are placed on it). */
    public Optional<DeliveryZone> zone() {
        if (zoneType == ZoneType.POLYGON && zonePolygon != null) {
            return Optional.of(new DeliveryZone.Polygon(zonePolygon));
        }
        if (zoneType == ZoneType.CIRCLE && zoneCenterLat != null && zoneCenterLng != null && zoneRadiusKm != null) {
            return Optional.of(new DeliveryZone.Circle(
                    new GeoPoint(zoneCenterLat.doubleValue(), zoneCenterLng.doubleValue()), zoneRadiusKm.doubleValue()));
        }
        return Optional.empty();
    }

    public void setZone(DeliveryZone zone) {
        zoneType = null;
        zonePolygon = null;
        zoneCenterLat = null;
        zoneCenterLng = null;
        zoneRadiusKm = null;
        switch (zone) {
            case null -> {
            }
            case DeliveryZone.Polygon polygon -> {
                zoneType = ZoneType.POLYGON;
                zonePolygon = polygon.vertices().stream()
                        .map(p -> new GeoPoint(round(p.lat(), 6), round(p.lng(), 6)))
                        .toList();
            }
            case DeliveryZone.Circle circle -> {
                zoneType = ZoneType.CIRCLE;
                zoneCenterLat = BigDecimal.valueOf(circle.center().lat()).setScale(6, RoundingMode.HALF_UP);
                zoneCenterLng = BigDecimal.valueOf(circle.center().lng()).setScale(6, RoundingMode.HALF_UP);
                zoneRadiusKm = BigDecimal.valueOf(circle.radiusKm()).setScale(3, RoundingMode.HALF_UP);
            }
        }
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArea() {
        return area;
    }

    public DeliveryDays getDays() {
        return DeliveryDays.fromCodes(days);
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getColor() {
        return color;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
