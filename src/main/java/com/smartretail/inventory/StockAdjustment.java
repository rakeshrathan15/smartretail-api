package com.smartretail.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** A manual change to stock and why it was made. To undo one, make the opposite adjustment. */
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {

    public enum Reason {
        DAMAGE,
        EXPIRED,
        CORRECTION,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false, updatable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Reason reason;

    @Column(updatable = false)
    private String notes;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected StockAdjustment() {
    }

    StockAdjustment(UUID productId, int delta, Reason reason, String notes, UUID createdBy) {
        this.productId = productId;
        this.delta = delta;
        this.reason = reason;
        this.notes = notes;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }
}
