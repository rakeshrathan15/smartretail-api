package com.smartretail.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Shop owner details; shares its primary key with {@link User}. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name")
    private String fullName;

    @Getter(AccessLevel.NONE)
    private String email;

    private String phone;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "shop_address")
    private String shopAddress;

    @Column(name = "shop_city")
    private String shopCity;

    @Column(name = "shop_state")
    private String shopState;

    @Column(name = "shop_pincode")
    private String shopPincode;

    @Column(name = "shop_lat")
    private BigDecimal shopLat;

    @Column(name = "shop_lng")
    private BigDecimal shopLng;

    @Column(name = "shop_place_id")
    @Getter(AccessLevel.NONE)
    private String shopPlaceId;

    @Column(name = "shop_profile_complete", nullable = false)
    private boolean shopProfileComplete;

    @Column(name = "delivery_route_id")
    private UUID deliveryRouteId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter(AccessLevel.NONE)
    private Instant updatedAt;

    public Profile(UUID userId, String fullName, String email, String phone) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    /** Saves the shop details and marks the profile complete. */
    public void updateShop(String shopName, String ownerName, String phone, String gstNumber, String address,
            String city, String state, String pincode, BigDecimal lat, BigDecimal lng, String placeId) {
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.phone = phone;
        this.gstNumber = gstNumber;
        this.shopAddress = address;
        this.shopCity = city;
        this.shopState = state;
        this.shopPincode = pincode;
        this.shopLat = lat;
        this.shopLng = lng;
        this.shopPlaceId = placeId;
        this.shopProfileComplete = true;
    }

    /** Set by route assignment from the shop's map pin; null when no route covers it. */
    public void assignDeliveryRoute(UUID routeId) {
        this.deliveryRouteId = routeId;
    }
}
