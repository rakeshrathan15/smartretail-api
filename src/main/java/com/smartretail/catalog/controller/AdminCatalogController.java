package com.smartretail.catalog.controller;

import com.smartretail.catalog.dto.request.BannerRequest;
import com.smartretail.catalog.dto.request.CategoryRequest;
import com.smartretail.catalog.dto.request.ProductRequest;
import com.smartretail.catalog.dto.request.ReorderRequest;
import com.smartretail.catalog.dto.request.SlabsRequest;
import com.smartretail.catalog.dto.response.AdminProductResponse;
import com.smartretail.catalog.dto.response.AdminProductRow;
import com.smartretail.catalog.dto.response.BannerResponse;
import com.smartretail.catalog.dto.response.CategoryResponse;
import com.smartretail.catalog.enums.ProductSort;
import com.smartretail.catalog.service.AdminCatalogService;
import com.smartretail.common.response.ApiResponse;
import com.smartretail.common.response.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Admin-only (enforced in SecurityConfig for /api/admin/**). */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final AdminCatalogService admin;

    // ---- Categories ------------------------------------------------------

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> categories() {
        return ApiResponse.ok(admin.listCategories());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(admin.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(admin.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id) {
        admin.deleteCategory(id);
    }

    // ---- Products --------------------------------------------------------

    @GetMapping("/products")
    public ApiResponse<PageResponse<AdminProductRow>> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "NAME") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(admin.listProducts(category, q, active, sort, page, size));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<AdminProductResponse> product(@PathVariable UUID id) {
        return ApiResponse.ok(admin.getProduct(id));
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(admin.createProduct(request));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<AdminProductResponse> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(admin.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        admin.deleteProduct(id);
    }

    @PutMapping("/products/{id}/slabs")
    public ApiResponse<AdminProductResponse> replaceSlabs(@PathVariable UUID id, @Valid @RequestBody SlabsRequest request) {
        return ApiResponse.ok(admin.replaceSlabs(id, request.slabs()));
    }

    @PostMapping(path = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminProductResponse> addImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(admin.addImage(id, file));
    }

    @PutMapping("/products/{id}/images/{imageId}/primary")
    public ApiResponse<AdminProductResponse> makePrimaryImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        return ApiResponse.ok(admin.makePrimaryImage(id, imageId));
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    public ApiResponse<AdminProductResponse> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        return ApiResponse.ok(admin.deleteImage(id, imageId));
    }

    // ---- Banners ---------------------------------------------------------

    @GetMapping("/banners")
    public ApiResponse<List<BannerResponse>> banners() {
        return ApiResponse.ok(admin.listBanners());
    }

    @PostMapping("/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BannerResponse> createBanner(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok(admin.createBanner(request));
    }

    @PutMapping("/banners/{id}")
    public ApiResponse<BannerResponse> updateBanner(@PathVariable UUID id, @Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok(admin.updateBanner(id, request));
    }

    @PostMapping(path = "/banners/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BannerResponse> uploadBannerImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(admin.uploadBannerImage(id, file));
    }

    @PutMapping("/banners/order")
    public ApiResponse<List<BannerResponse>> reorderBanners(@Valid @RequestBody ReorderRequest request) {
        return ApiResponse.ok(admin.reorderBanners(request.ids()));
    }

    @DeleteMapping("/banners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(@PathVariable UUID id) {
        admin.deleteBanner(id);
    }
}
