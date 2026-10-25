package com.smartretail.catalog;

import com.smartretail.catalog.CatalogDtos.AdminProductResponse;
import com.smartretail.catalog.CatalogDtos.AdminProductRow;
import com.smartretail.catalog.CatalogDtos.CategoryResponse;
import com.smartretail.catalog.CatalogDtos.ImageResponse;
import com.smartretail.catalog.CatalogDtos.ProductCardResponse;
import com.smartretail.catalog.CatalogDtos.ProductDetailResponse;
import com.smartretail.catalog.CatalogDtos.SlabResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Turns products into API responses, loading thumbnails and slab prices for a whole page at once. */
@Component
class ProductMapper {

    private final ProductImageRepository images;
    private final PriceSlabRepository slabs;

    ProductMapper(ProductImageRepository images, PriceSlabRepository slabs) {
        this.images = images;
        this.slabs = slabs;
    }

    List<ProductCardResponse> toCards(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = products.stream().map(Product::getId).toList();

        Map<UUID, String> thumbnails = new HashMap<>();
        for (ProductImage image : images.findForProducts(ids)) {
            thumbnails.putIfAbsent(image.getProduct().getId(), image.getUrl());
        }
        Map<UUID, BigDecimal> lowest = new HashMap<>();
        for (PriceSlabRepository.LowestSlabPrice row : slabs.findLowestPrices(ids)) {
            lowest.put(row.getProductId(), row.getLowest());
        }

        return products.stream().map(p -> {
            BigDecimal slabLow = lowest.get(p.getId());
            BigDecimal lowestPrice = slabLow != null && slabLow.compareTo(p.getPrice()) < 0 ? slabLow : p.getPrice();
            return new ProductCardResponse(
                    p.getId(), p.getSlug(), p.getName(), p.getBrand(),
                    p.getCategory() == null ? null : p.getCategory().getId(),
                    p.getUnitVolumeMl(), p.getUnitsPerCase(), p.getPrice(), p.getOriginalPrice(), lowestPrice,
                    thumbnails.get(p.getId()), p.isInStock(), p.getBadge());
        }).toList();
    }

    /** Needs an open transaction: reads each product's category. */
    List<AdminProductRow> toAdminRows(List<Product> products) {
        List<ProductCardResponse> cards = toCards(products);
        List<AdminProductRow> rows = new ArrayList<>(products.size());
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            ProductCardResponse card = cards.get(i);
            rows.add(new AdminProductRow(p.getId(), p.getSlug(), p.getName(), p.getBrand(),
                    p.getCategory() == null ? null : p.getCategory().getName(), p.getUnitsPerCase(), p.getPrice(),
                    card.lowestPrice(), card.imageUrl(), p.getStockQuantity(), p.getLowStockThreshold(), p.isActive()));
        }
        return rows;
    }

    /** Needs an open transaction: reads the product's category, images and slabs. */
    ProductDetailResponse toDetail(Product p) {
        return new ProductDetailResponse(
                p.getId(), p.getSlug(), p.getName(), p.getBrand(), p.getFlavor(),
                p.getCategory() == null ? null : CategoryResponse.from(p.getCategory()),
                p.getUnitVolumeMl(), p.getUnitsPerCase(), p.getWeight(),
                p.getPrice(), p.getOriginalPrice(), p.lowestPricePerCase(), p.isInStock(), p.getBadge(),
                p.getDescription(), p.getIngredients(),
                p.getImages().stream().map(ImageResponse::from).toList(),
                p.getSlabs().stream().map(SlabResponse::from).toList());
    }

    AdminProductResponse toAdmin(Product p) {
        return new AdminProductResponse(toDetail(p), p.getCostPrice(), p.getStockQuantity(),
                p.getLowStockThreshold(), p.getBarcode(), p.isActive());
    }
}
