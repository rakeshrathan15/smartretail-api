package com.smartretail.supplier.entity;

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

/** A company stock is bought from. Turned off rather than deleted, so past receipts keep their name. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "suppliers")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String phone;

    private String gstin;

    private String address;

    private String notes;

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

    public Supplier(String name) {
        this.name = name;
    }

    public void update(String name, String phone, String gstin, String address, String notes) {
        this.name = name;
        this.phone = phone;
        this.gstin = gstin;
        this.address = address;
        this.notes = notes;
    }
}
