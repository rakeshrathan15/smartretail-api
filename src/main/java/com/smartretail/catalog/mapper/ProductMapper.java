package com.smartretail.catalog.mapper;

import com.smartretail.catalog.dto.response.AdminProductResponse;
import com.smartretail.catalog.dto.response.AdminProductRow;
import com.smartretail.catalog.dto.response.ProductCardResponse;
import com.smartretail.catalog.dto.response.ProductDetailResponse;
import com.smartretail.catalog.entity.Product;
import com.smartretail.catalog.entity.ProductImage;
import com.smartretail.catalog.repository.PriceSlabRepository;
import com.smartretail.common.mapper.MapStructConfig;
import com.smartretail.catalog.repository.ProductImageRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

/** Turns products into API responses, loading thumbnails and slab prices for a whole page at once. */
@Mapper(config = MapStructConfig.class, uses = CatalogMapper.class)
public abstract class ProductMapper {

    @Autowired
    protected ProductImageRepository images;

    @Autowired
    protected PriceSlabRepository slabs;

    public List<ProductCardResponse> toCards(List<Product> products) {
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
    public List<AdminProductRow> toAdminRows(List<Product> products) {
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
    @Mapping(target = "lowestPrice", expression = "java(p.lowestPricePerCase())")
    public abstract ProductDetailResponse toDetail(Product p);

    @Mapping(target = "product", source = ".")
    public abstract AdminProductResponse toAdmin(Product p);
}
