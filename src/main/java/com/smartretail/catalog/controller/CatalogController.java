package com.smartretail.catalog.controller;

import com.smartretail.catalog.dto.response.BannerResponse;
import com.smartretail.catalog.dto.response.CategoryResponse;
import com.smartretail.catalog.dto.response.ProductCardResponse;
import com.smartretail.catalog.dto.response.ProductDetailResponse;
import com.smartretail.catalog.enums.ProductSort;
import com.smartretail.catalog.service.CatalogService;
import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.response.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public storefront endpoints; no login needed. */
@RequiredArgsConstructor
@RestController
public class CatalogController {

    private final CatalogService catalog;

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(catalog.listCategories());
    }

    /**
     * @param category category slug, e.g. "colas"
     * @param q        search in name, brand and flavor
     * @param deals    only products priced below their original price
     */
    @GetMapping("/api/products")
    public ApiResponse<PageResponse<ProductCardResponse>> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean deals,
            @RequestParam(defaultValue = "POPULAR") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(catalog.listProducts(category, q, deals, sort, page, size));
    }

    @GetMapping("/api/products/{slug}")
    public ApiResponse<ProductDetailResponse> product(@PathVariable String slug) {
        return ApiResponse.ok(catalog.getProduct(slug));
    }

    @GetMapping("/api/banners")
    public ApiResponse<List<BannerResponse>> banners() {
        return ApiResponse.ok(catalog.listBanners());
    }
}
