package com.smartretail.route.entity;

import com.smartretail.route.support.DeliveryDays;
import com.smartretail.route.support.DeliveryZone;
import com.smartretail.route.support.GeoPoint;
import com.smartretail.route.support.GeoPointListConverter;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** A van run: the area it covers, the days it goes and the store it leaves from. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "delivery_routes")
public class DeliveryRoute {

    public enum ZoneType {
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
    @Getter(AccessLevel.NONE)
    private ZoneType zoneType;

    @Convert(converter = GeoPointListConverter.class)
    @Column(name = "zone_polygon", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    @Getter(AccessLevel.NONE)
    private List<GeoPoint> zonePolygon;

    @Column(name = "zone_center_lat")
    @Getter(AccessLevel.NONE)
    private BigDecimal zoneCenterLat;

    @Column(name = "zone_center_lng")
    @Getter(AccessLevel.NONE)
    private BigDecimal zoneCenterLng;

    @Column(name = "zone_radius_km")
    @Getter(AccessLevel.NONE)
    private BigDecimal zoneRadiusKm;

    @Column(nullable = false)
    @Setter
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter(AccessLevel.NONE)
    private Instant updatedAt;

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

    public DeliveryDays getDays() {
        return DeliveryDays.fromCodes(days);
    }
}
