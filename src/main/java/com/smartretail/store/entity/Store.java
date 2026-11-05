package com.smartretail.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A warehouse or branch that delivery routes leave from. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
}
