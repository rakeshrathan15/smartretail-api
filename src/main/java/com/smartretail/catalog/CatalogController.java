package com.smartretail.catalog;

import com.smartretail.catalog.CatalogDtos.BannerResponse;
import com.smartretail.catalog.CatalogDtos.CategoryResponse;
import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.catalog.CatalogDtos.ProductCardResponse;
import com.smartretail.catalog.CatalogDtos.ProductDetailResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public storefront endpoints; no login needed. */
@RestController
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return catalog.listCategories();
    }

    /**
     * @param category category slug, e.g. "colas"
     * @param q        search in name, brand and flavor
     * @param deals    only products priced below their original price
     */
    @GetMapping("/api/products")
    public PageResponse<ProductCardResponse> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean deals,
            @RequestParam(defaultValue = "POPULAR") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.listProducts(category, q, deals, sort, page, size);
    }

    @GetMapping("/api/products/{slug}")
    public ProductDetailResponse product(@PathVariable String slug) {
        return catalog.getProduct(slug);
    }

    @GetMapping("/api/banners")
    public List<BannerResponse> banners() {
        return catalog.listBanners();
    }
}
