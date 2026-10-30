package com.smartretail.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A kirana buyer. A credit limit above zero lets its owner order on credit. */
@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String gstin;

    @Column(name = "address_line1")
    private String addressLine1;

    private String city;

    private String pincode;

    @Column(name = "delivery_route_id")
    private UUID deliveryRouteId;

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Shop() {
    }

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

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UUID getDeliveryRouteId() {
        return deliveryRouteId;
    }

    public void assignDeliveryRoute(UUID routeId) {
        this.deliveryRouteId = routeId;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
