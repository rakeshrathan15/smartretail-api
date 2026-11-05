package com.smartretail.shop.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** Money received from a shop against what it owes. Never deleted; a wrong entry is voided. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
    @Getter(AccessLevel.NONE)
    private UUID shopId;

    @Column(nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private Method method;

    @Column(updatable = false)
    @Getter(AccessLevel.NONE)
    private String reference;

    @Column(name = "received_by", updatable = false)
    @Getter(AccessLevel.NONE)
    private UUID receivedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Getter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "voided_at")
    @Getter(AccessLevel.NONE)
    private Instant voidedAt;

    @Column(name = "voided_by")
    @Getter(AccessLevel.NONE)
    private UUID voidedBy;

    public ShopPayment(UUID shopId, BigDecimal amount, Method method, String reference, UUID receivedBy) {
        this.shopId = shopId;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.receivedBy = receivedBy;
    }

    public boolean isVoided() {
        return voidedAt != null;
    }

    public void voidBy(UUID adminId, Instant when) {
        this.voidedAt = when;
        this.voidedBy = adminId;
    }
}
