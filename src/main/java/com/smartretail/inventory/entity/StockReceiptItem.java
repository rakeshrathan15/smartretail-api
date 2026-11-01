package com.smartretail.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stock_receipt_items")
public class StockReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private UUID productId;

    @Column(nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private int quantity;

    @Column(name = "cost_price", nullable = false, updatable = false)
    @Getter(AccessLevel.NONE)
    private BigDecimal costPrice;

    @Column(nullable = false, updatable = false)
    private BigDecimal total;

    public StockReceiptItem(UUID productId, int quantity, BigDecimal costPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.total = costPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
