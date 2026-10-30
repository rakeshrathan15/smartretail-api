package com.smartretail.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A warehouse or branch that delivery routes leave from. */
@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String gstin;

    private String phone;

    private String email;

    private String address;

    private String city;

    private String pincode;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Store() {
    }

    public Store(String name) {
        this.name = name;
    }

    public void update(String name, String gstin, String phone, String email, String address, String city,
            String pincode) {
        this.name = name;
        this.gstin = gstin;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.city = city;
        this.pincode = pincode;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGstin() {
        return gstin;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getPincode() {
        return pincode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
