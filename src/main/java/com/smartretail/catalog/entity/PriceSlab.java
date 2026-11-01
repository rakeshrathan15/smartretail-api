package com.smartretail.catalog.entity;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Bulk price: ordering at least {@code minQty} cases costs {@code pricePerCase} each. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "price_slabs")
public class PriceSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    @Getter(AccessLevel.NONE)
    private Product product;

    @Column(name = "min_qty", nullable = false)
    private int minQty;

    @Column(name = "max_qty")
    private Integer maxQty;

    @Column(name = "price_per_case", nullable = false)
    private BigDecimal pricePerCase;

    public PriceSlab(Product product, int minQty, Integer maxQty, BigDecimal pricePerCase) {
        this.product = product;
        this.minQty = minQty;
        this.maxQty = maxQty;
        this.pricePerCase = pricePerCase;
    }
}
