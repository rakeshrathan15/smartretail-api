package com.smartretail.catalog.service;

import com.smartretail.catalog.dto.response.BannerResponse;
import com.smartretail.catalog.dto.response.CategoryResponse;
import com.smartretail.catalog.dto.response.ProductCardResponse;
import com.smartretail.catalog.dto.response.ProductDetailResponse;
import com.smartretail.catalog.entity.Product;
import com.smartretail.catalog.enums.ProductSort;
import com.smartretail.catalog.mapper.CatalogMapper;
import com.smartretail.catalog.mapper.ProductMapper;
import com.smartretail.catalog.repository.BannerRepository;
import com.smartretail.catalog.repository.CategoryRepository;
import com.smartretail.catalog.repository.ProductRepository;
import com.smartretail.catalog.support.ProductFilter;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.response.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only catalog for the storefront. Only active items are visible. */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CatalogService {

    public static final int MAX_PAGE_SIZE = 50;

    private final CatalogMapper catalogMapper;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final BannerRepository banners;
    private final ProductMapper mapper;

    public List<CategoryResponse> listCategories() {
        return categories.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream().map(catalogMapper::toCategory).toList();
    }

    public PageResponse<ProductCardResponse> listProducts(String categorySlug, String query, boolean dealsOnly,
            ProductSort sort, int page, int size) {
        ProductFilter filter = new ProductFilter(categorySlug, query, dealsOnly, null, true);
        Page<Product> result = products.findAll(filter.toSpecification(), pageRequest(page, size, sort));
        return new PageResponse<>(mapper.toCards(result.getContent()), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    public ProductDetailResponse getProduct(String slug) {
        return products.findBySlug(slug)
                .filter(p -> p.isActive() && (p.getCategory() == null || p.getCategory().isActive()))
                .map(mapper::toDetail)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
    }

    public List<BannerResponse> listBanners() {
        return banners.findAllByActiveTrueOrderBySortOrderAsc().stream().map(catalogMapper::toBanner).toList();
    }

    public static PageRequest pageRequest(int page, int size, ProductSort sort) {
        if (page < 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE, "page must be 0 or more");
        }
        int boundedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(page, boundedSize, (sort == null ? ProductSort.POPULAR : sort).sort());
    }
}
