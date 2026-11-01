package com.smartretail.catalog.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A product sold by the case. {@code price} is the base price per case before bulk slabs. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @Setter
    private Category category;

    @Column(nullable = false)
    @Setter
    private String name;

    @Column(nullable = false)
    @Setter
    private String slug;

    @Setter
    private String brand;

    @Setter
    private String flavor;

    @Column(name = "unit_volume_ml")
    @Setter
    private Integer unitVolumeMl;

    @Column(name = "units_per_case", nullable = false)
    @Setter
    private int unitsPerCase = 1;

    @Setter
    private String weight;

    @Column(nullable = false)
    @Getter(AccessLevel.NONE)
    private String unit = "case";

    @Column(nullable = false)
    @Setter
    private BigDecimal price;

    @Column(name = "original_price")
    @Setter
    private BigDecimal originalPrice;

    @Column(name = "cost_price")
    @Setter
    private BigDecimal costPrice;

    @Column(name = "stock_quantity", nullable = false)
    @Setter
    private int stockQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    @Setter
    private int lowStockThreshold = 5;

    @Setter
    private String barcode;

    @Setter
    private String badge;

    @Setter
    private String description;

    @Setter
    private String ingredients;

    @Column(nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "is_active", nullable = false)
    @Setter
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("minQty ASC")
    private List<PriceSlab> slabs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Getter(AccessLevel.NONE)
    private Instant updatedAt;

    public Product(String name, String slug, BigDecimal price) {
        this.name = name;
        this.slug = slug;
        this.price = price;
    }

    /** Price per case for an order of {@code cases}: the slab with the highest min qty that applies. */
    public BigDecimal pricePerCaseFor(int cases) {
        return slabs.stream()
                .filter(s -> s.getMinQty() <= cases)
                .max(Comparator.comparingInt(PriceSlab::getMinQty))
                .map(PriceSlab::getPricePerCase)
                .orElse(price);
    }

    /** The cheapest price per case reachable with bulk slabs ("as low as"). */
    public BigDecimal lowestPricePerCase() {
        return slabs.stream().map(PriceSlab::getPricePerCase).min(BigDecimal::compareTo)
                .filter(p -> p.compareTo(price) < 0)
                .orElse(price);
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public Optional<ProductImage> primaryImage() {
        return images.stream().filter(ProductImage::isPrimary).findFirst()
                .or(() -> images.stream().findFirst());
    }

    public ProductImage addImage(String url) {
        ProductImage image = new ProductImage(this, url, images.isEmpty(), images.size());
        images.add(image);
        return image;
    }

    public void makePrimary(ProductImage primary) {
        images.forEach(i -> i.setPrimary(i == primary));
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        if (image.isPrimary() && !images.isEmpty()) {
            images.getFirst().setPrimary(true);
        }
    }

    public void addSlab(int minQty, Integer maxQty, BigDecimal pricePerCase) {
        slabs.add(new PriceSlab(this, minQty, maxQty, pricePerCase));
    }

    /** Cases arrived from a supplier. The latest buying price becomes the product's cost. */
    public void receiveStock(int cases, BigDecimal unitCost) {
        this.stockQuantity += cases;
        this.costPrice = unitCost;
    }

    public void adjustStock(int delta) {
        this.stockQuantity += delta;
    }
}
