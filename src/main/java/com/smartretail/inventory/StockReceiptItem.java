package com.smartretail.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "stock_receipt_items")
public class StockReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "cost_price", nullable = false, updatable = false)
    private BigDecimal costPrice;

    @Column(nullable = false, updatable = false)
    private BigDecimal total;

    protected StockReceiptItem() {
    }

    StockReceiptItem(UUID productId, int quantity, BigDecimal costPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.total = costPrice.multiply(BigDecimal.valueOf(quantity));
    }

    BigDecimal getTotal() {
        return total;
    }
}
