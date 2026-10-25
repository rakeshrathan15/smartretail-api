package com.smartretail.catalog;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A product sold by the case. {@code price} is the base price per case before bulk slabs. */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    private String brand;

    private String flavor;

    @Column(name = "unit_volume_ml")
    private Integer unitVolumeMl;

    @Column(name = "units_per_case", nullable = false)
    private int unitsPerCase = 1;

    private String weight;

    @Column(nullable = false)
    private String unit = "case";

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    @Column(name = "cost_price")
    private BigDecimal costPrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    private String barcode;

    private String badge;

    private String description;

    private String ingredients;

    @Column(nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "is_active", nullable = false)
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
    private Instant updatedAt;

    protected Product() {
    }

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

    ProductImage addImage(String url) {
        ProductImage image = new ProductImage(this, url, images.isEmpty(), images.size());
        images.add(image);
        return image;
    }

    void makePrimary(ProductImage primary) {
        images.forEach(i -> i.setPrimary(i == primary));
    }

    void removeImage(ProductImage image) {
        images.remove(image);
        if (image.isPrimary() && !images.isEmpty()) {
            images.getFirst().setPrimary(true);
        }
    }

    void addSlab(int minQty, Integer maxQty, BigDecimal pricePerCase) {
        slabs.add(new PriceSlab(this, minQty, maxQty, pricePerCase));
    }

    public UUID getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public Integer getUnitVolumeMl() {
        return unitVolumeMl;
    }

    public void setUnitVolumeMl(Integer unitVolumeMl) {
        this.unitVolumeMl = unitVolumeMl;
    }

    public int getUnitsPerCase() {
        return unitsPerCase;
    }

    public void setUnitsPerCase(int unitsPerCase) {
        this.unitsPerCase = unitsPerCase;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public List<PriceSlab> getSlabs() {
        return slabs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
