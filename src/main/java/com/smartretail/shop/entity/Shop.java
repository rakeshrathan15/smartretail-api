package com.smartretail.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A kirana buyer. A credit limit above zero lets its owner order on credit. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(nullable = false)
    @Setter
    private String name;

    @Setter
    private String phone;

    @Getter(AccessLevel.NONE)
    private String gstin;

    @Column(name = "address_line1")
    @Getter(AccessLevel.NONE)
    private String addressLine1;

    @Getter(AccessLevel.NONE)
    private String city;

    @Getter(AccessLevel.NONE)
    private String pincode;

    @Column(name = "delivery_route_id")
    private UUID deliveryRouteId;

    @Column(name = "credit_limit", nullable = false)
    @Setter
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Setter
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter(AccessLevel.NONE)
    private Instant updatedAt;

    public Shop(UUID ownerUserId, String name) {
        this.ownerUserId = ownerUserId;
        this.name = name;
    }

    public void updateDetails(String name, String phone, String gstin, String address, String city, String pincode) {
        this.name = name;
        this.phone = phone;
        this.gstin = gstin;
        this.addressLine1 = address;
        this.city = city;
        this.pincode = pincode;
    }

    public void assignDeliveryRoute(UUID routeId) {
        this.deliveryRouteId = routeId;
    }
}
