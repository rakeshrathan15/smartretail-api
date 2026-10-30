package com.smartretail.shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/** Money received from a shop against what it owes. Never deleted; a wrong entry is voided. */
@Entity
@Table(name = "shop_payments")
public class ShopPayment {

    public enum Method {
        CASH,
        UPI,
        BANK,
        CHEQUE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shop_id", nullable = false, updatable = false)
    private UUID shopId;

    @Column(nullable = false, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Method method;

    @Column(updatable = false)
    private String reference;

    @Column(name = "received_by", updatable = false)
    private UUID receivedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;

    protected ShopPayment() {
    }

    ShopPayment(UUID shopId, BigDecimal amount, Method method, String reference, UUID receivedBy) {
        this.shopId = shopId;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.receivedBy = receivedBy;
    }

    boolean isVoided() {
        return voidedAt != null;
    }

    void voidBy(UUID adminId, Instant when) {
        this.voidedAt = when;
        this.voidedBy = adminId;
    }

    public UUID getId() {
        return id;
    }
}
