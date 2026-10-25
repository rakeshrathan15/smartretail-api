package com.smartretail.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Shop owner details; shares its primary key with {@link User}. */
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name")
    private String fullName;

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
    private String shopPlaceId;

    @Column(name = "shop_profile_complete", nullable = false)
    private boolean shopProfileComplete;

    @Column(name = "delivery_route_id")
    private UUID deliveryRouteId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Profile() {
    }

    public Profile(UUID userId, String fullName, String email, String phone) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getShopName() {
        return shopName;
    }

    public boolean isShopProfileComplete() {
        return shopProfileComplete;
    }

    public UUID getDeliveryRouteId() {
        return deliveryRouteId;
    }
}
