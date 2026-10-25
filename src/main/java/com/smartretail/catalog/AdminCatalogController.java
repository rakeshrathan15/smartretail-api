package com.smartretail.catalog;

import com.smartretail.catalog.CatalogDtos.AdminProductResponse;
import com.smartretail.catalog.CatalogDtos.AdminProductRow;
import com.smartretail.catalog.CatalogDtos.BannerRequest;
import com.smartretail.catalog.CatalogDtos.BannerResponse;
import com.smartretail.catalog.CatalogDtos.CategoryRequest;
import com.smartretail.catalog.CatalogDtos.CategoryResponse;
import com.smartretail.catalog.CatalogDtos.PageResponse;
import com.smartretail.catalog.CatalogDtos.ProductRequest;
import com.smartretail.catalog.CatalogDtos.ReorderRequest;
import com.smartretail.catalog.CatalogDtos.SlabsRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final AdminCatalogService admin;

    public AdminCatalogController(AdminCatalogService admin) {
        this.admin = admin;
    }

    // ---- Categories ------------------------------------------------------

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return admin.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return admin.createCategory(request);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return admin.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable UUID id) {
        admin.deleteCategory(id);
    }

    // ---- Products --------------------------------------------------------

    @GetMapping("/products")
    public PageResponse<AdminProductRow> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "NAME") ProductSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return admin.listProducts(category, q, active, sort, page, size);
    }

    @GetMapping("/products/{id}")
    public AdminProductResponse product(@PathVariable UUID id) {
        return admin.getProduct(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return admin.createProduct(request);
    }

    @PutMapping("/products/{id}")
    public AdminProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return admin.updateProduct(id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        admin.deleteProduct(id);
    }

    @PutMapping("/products/{id}/slabs")
    public AdminProductResponse replaceSlabs(@PathVariable UUID id, @Valid @RequestBody SlabsRequest request) {
        return admin.replaceSlabs(id, request.slabs());
    }

    @PostMapping(path = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminProductResponse addImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return admin.addImage(id, file);
    }

    @PutMapping("/products/{id}/images/{imageId}/primary")
    public AdminProductResponse makePrimaryImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        return admin.makePrimaryImage(id, imageId);
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    public AdminProductResponse deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        return admin.deleteImage(id, imageId);
    }

    // ---- Banners ---------------------------------------------------------

    @GetMapping("/banners")
    public List<BannerResponse> banners() {
        return admin.listBanners();
    }

    @PostMapping("/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public BannerResponse createBanner(@Valid @RequestBody BannerRequest request) {
        return admin.createBanner(request);
    }

    @PutMapping("/banners/{id}")
    public BannerResponse updateBanner(@PathVariable UUID id, @Valid @RequestBody BannerRequest request) {
        return admin.updateBanner(id, request);
    }

    @PostMapping(path = "/banners/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BannerResponse uploadBannerImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return admin.uploadBannerImage(id, file);
    }

    @PutMapping("/banners/order")
    public List<BannerResponse> reorderBanners(@Valid @RequestBody ReorderRequest request) {
        return admin.reorderBanners(request.ids());
    }

    @DeleteMapping("/banners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(@PathVariable UUID id) {
        admin.deleteBanner(id);
    }
}
