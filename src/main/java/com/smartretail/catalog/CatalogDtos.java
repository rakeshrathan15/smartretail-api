package com.smartretail.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Request and response bodies for the catalog APIs. Prices are rupees per case. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    // ---- Responses -------------------------------------------------------

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }

    public record CategoryResponse(UUID id, String name, String slug, String imageUrl, boolean active, int sortOrder) {

        static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getImageUrl(), c.isActive(),
                    c.getSortOrder());
        }
    }

    /** A product as shown in lists and grids. */
    public record ProductCardResponse(
            UUID id,
            String slug,
            String name,
            String brand,
            UUID categoryId,
            Integer unitVolumeMl,
            int unitsPerCase,
            BigDecimal price,
            BigDecimal originalPrice,
            BigDecimal lowestPrice,
            String imageUrl,
            boolean inStock,
            String badge) {
    }

    public record ImageResponse(UUID id, String url, boolean primary) {

        static ImageResponse from(ProductImage i) {
            return new ImageResponse(i.getId(), i.getUrl(), i.isPrimary());
        }
    }

    public record SlabResponse(int minQty, Integer maxQty, BigDecimal pricePerCase) {

        static SlabResponse from(PriceSlab s) {
            return new SlabResponse(s.getMinQty(), s.getMaxQty(), s.getPricePerCase());
        }
    }

    public record ProductDetailResponse(
            UUID id,
            String slug,
            String name,
            String brand,
            String flavor,
            CategoryResponse category,
            Integer unitVolumeMl,
            int unitsPerCase,
            String weight,
            BigDecimal price,
            BigDecimal originalPrice,
            BigDecimal lowestPrice,
            boolean inStock,
            String badge,
            String description,
            String ingredients,
            List<ImageResponse> images,
            List<SlabResponse> slabs) {
    }

    /** A row in the admin product table. */
    public record AdminProductRow(
            UUID id,
            String slug,
            String name,
            String brand,
            String categoryName,
            int unitsPerCase,
            BigDecimal price,
            BigDecimal lowestPrice,
            String imageUrl,
            int stockQuantity,
            int lowStockThreshold,
            boolean active) {
    }

    /** Admin view: adds cost, stock and status fields that customers never see. */
    public record AdminProductResponse(
            ProductDetailResponse product,
            BigDecimal costPrice,
            int stockQuantity,
            int lowStockThreshold,
            String barcode,
            boolean active) {
    }

    public record BannerResponse(
            UUID id, String title, String subtitle, String ctaText, String link, String imageUrl,
            boolean active, int sortOrder) {

        static BannerResponse from(Banner b) {
            return new BannerResponse(b.getId(), b.getTitle(), b.getSubtitle(), b.getCtaText(), b.getLink(),
                    b.getImageUrl(), b.isActive(), b.getSortOrder());
        }
    }

    // ---- Requests --------------------------------------------------------

    public record CategoryRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 500) String imageUrl,
            Boolean active,
            Integer sortOrder) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 150) String name,
            UUID categoryId,
            @Size(max = 80) String brand,
            @Size(max = 80) String flavor,
            @Positive Integer unitVolumeMl,
            @NotNull @Positive Integer unitsPerCase,
            @Size(max = 40) String weight,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @DecimalMin("0.00") BigDecimal originalPrice,
            @DecimalMin("0.00") BigDecimal costPrice,
            @PositiveOrZero Integer stockQuantity,
            @PositiveOrZero Integer lowStockThreshold,
            @Size(max = 64) String barcode,
            @Size(max = 30) String badge,
            @Size(max = 4000) String description,
            @Size(max = 4000) String ingredients,
            Boolean active) {
    }

    public record SlabRequest(
            @NotNull @Positive Integer minQty,
            @Positive Integer maxQty,
            @NotNull @DecimalMin("0.00") BigDecimal pricePerCase) {
    }

    public record SlabsRequest(@NotNull @Valid List<SlabRequest> slabs) {
    }

    public record BannerRequest(
            @NotBlank @Size(max = 120) String title,
            @Size(max = 200) String subtitle,
            @Size(max = 40) String ctaText,
            @Size(max = 300) String link,
            @Size(max = 500) String imageUrl,
            Boolean active) {
    }

    public record ReorderRequest(@NotNull List<UUID> ids) {
    }
}
