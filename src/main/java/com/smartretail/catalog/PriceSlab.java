package com.smartretail.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** Bulk price: ordering at least {@code minQty} cases costs {@code pricePerCase} each. */
@Entity
@Table(name = "price_slabs")
public class PriceSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "min_qty", nullable = false)
    private int minQty;

    @Column(name = "max_qty")
    private Integer maxQty;

    @Column(name = "price_per_case", nullable = false)
    private BigDecimal pricePerCase;

    protected PriceSlab() {
    }

    PriceSlab(Product product, int minQty, Integer maxQty, BigDecimal pricePerCase) {
        this.product = product;
        this.minQty = minQty;
        this.maxQty = maxQty;
        this.pricePerCase = pricePerCase;
    }

    public UUID getId() {
        return id;
    }

    public int getMinQty() {
        return minQty;
    }

    public Integer getMaxQty() {
        return maxQty;
    }

    public BigDecimal getPricePerCase() {
        return pricePerCase;
    }
}
