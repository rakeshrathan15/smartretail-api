package com.smartretail.catalog;

import com.smartretail.catalog.CatalogDtos.BannerResponse;
import com.smartretail.catalog.CatalogDtos.CategoryResponse;
import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.catalog.CatalogDtos.ProductCardResponse;
import com.smartretail.catalog.CatalogDtos.ProductDetailResponse;
import com.smartretail.common.ApiException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only catalog for the storefront. Only active items are visible. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    static final int MAX_PAGE_SIZE = 50;

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final BannerRepository banners;
    private final ProductMapper mapper;

    public CatalogService(CategoryRepository categories, ProductRepository products, BannerRepository banners,
            ProductMapper mapper) {
        this.categories = categories;
        this.products = products;
        this.banners = banners;
        this.mapper = mapper;
    }

    public List<CategoryResponse> listCategories() {
        return categories.findAllByActiveTrueOrderBySortOrderAscNameAsc().stream().map(CategoryResponse::from).toList();
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
                .orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    public List<BannerResponse> listBanners() {
        return banners.findAllByActiveTrueOrderBySortOrderAsc().stream().map(BannerResponse::from).toList();
    }

    static PageRequest pageRequest(int page, int size, ProductSort sort) {
        if (page < 0) {
            throw ApiException.badRequest("page must be 0 or more");
        }
        int boundedSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(page, boundedSize, (sort == null ? ProductSort.POPULAR : sort).sort());
    }
}
