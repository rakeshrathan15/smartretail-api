package com.smartretail.catalog.service;

import com.smartretail.catalog.dto.request.BannerRequest;
import com.smartretail.catalog.dto.request.CategoryRequest;
import com.smartretail.catalog.dto.request.ProductRequest;
import com.smartretail.catalog.dto.request.SlabRequest;
import com.smartretail.catalog.dto.response.AdminProductResponse;
import com.smartretail.catalog.dto.response.AdminProductRow;
import com.smartretail.catalog.dto.response.BannerResponse;
import com.smartretail.catalog.dto.response.CategoryResponse;
import com.smartretail.catalog.entity.Banner;
import com.smartretail.catalog.entity.Category;
import com.smartretail.catalog.entity.Product;
import com.smartretail.catalog.entity.ProductImage;
import com.smartretail.catalog.enums.ProductSort;
import com.smartretail.catalog.mapper.CatalogMapper;
import com.smartretail.catalog.mapper.ProductMapper;
import com.smartretail.catalog.repository.BannerRepository;
import com.smartretail.catalog.repository.CategoryRepository;
import com.smartretail.catalog.repository.ProductRepository;
import com.smartretail.catalog.support.ProductFilter;
import com.smartretail.catalog.support.Slugs;
import com.smartretail.common.exception.ApiException;
import com.smartretail.common.exception.ErrorCode;
import com.smartretail.common.response.PageResponse;
import com.smartretail.storage.service.StorageService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Catalog management for admins. Sees inactive items too. */
@RequiredArgsConstructor
@Service
@Transactional
public class AdminCatalogService {

    private final CatalogMapper catalogMapper;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final BannerRepository banners;
    private final ProductMapper mapper;
    private final StorageService storage;
    private final EntityManager entityManager;

    // ---- Categories ------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(catalogMapper::toCategory).toList();
    }

    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByNameIgnoreCase(name)) {
            throw new ApiException(ErrorCode.CATEGORY_ALREADY_EXISTS, "A category named \"" + name + "\" already exists");
        }
        Category category = new Category(name, Slugs.unique(name, categories::existsBySlug));
        applyCategory(category, request);
        return catalogMapper.toCategory(categories.save(category));
    }

    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = findCategory(id);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(category.getName())) {
            if (categories.existsByNameIgnoreCase(name)) {
                throw new ApiException(ErrorCode.CATEGORY_ALREADY_EXISTS, "A category named \"" + name + "\" already exists");
            }
            category.setSlug(Slugs.unique(name, categories::existsBySlug));
        }
        category.setName(name);
        applyCategory(category, request);
        return catalogMapper.toCategory(category);
    }

    public void deleteCategory(UUID id) {
        Category category = findCategory(id);
        long count = products.countByCategory(category);
        if (count > 0) {
            throw new ApiException(ErrorCode.CATEGORY_IN_USE, "Move or delete its " + count + " products first, or mark it inactive");
        }
        categories.delete(category);
    }

    private static void applyCategory(Category category, CategoryRequest request) {
        category.setImageUrl(blankToNull(request.imageUrl()));
        if (request.active() != null) {
            category.setActive(request.active());
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
    }

    private Category findCategory(UUID id) {
        return categories.findById(id).orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found"));
    }

    // ---- Products --------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<AdminProductRow> listProducts(String categorySlug, String query, Boolean active,
            ProductSort sort, int page, int size) {
        ProductFilter filter = new ProductFilter(categorySlug, query, false, active, false);
        Page<Product> result = products.findAll(filter.toSpecification(),
                CatalogService.pageRequest(page, size, sort == null ? ProductSort.NAME : sort));
        return new PageResponse<>(mapper.toAdminRows(result.getContent()), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getProduct(UUID id) {
        return mapper.toAdmin(findProduct(id));
    }

    public AdminProductResponse createProduct(ProductRequest request) {
        String name = request.name().trim();
        Product product = new Product(name, Slugs.unique(name, products::existsBySlug), request.price());
        applyProduct(product, request);
        product.setStockQuantity(Objects.requireNonNullElse(request.stockQuantity(), 0));
        return mapper.toAdmin(products.save(product));
    }

    public AdminProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = findProduct(id);
        String name = request.name().trim();
        if (!name.equals(product.getName())) {
            product.setName(name);
            product.setSlug(Slugs.unique(name, slug -> !slug.equals(product.getSlug()) && products.existsBySlug(slug)));
        }
        applyProduct(product, request);
        validateSlabs(product.getSlabs().stream()
                .map(s -> new SlabRequest(s.getMinQty(), s.getMaxQty(), s.getPricePerCase())).toList(),
                product.getPrice());
        return mapper.toAdmin(product);
    }

    public void deleteProduct(UUID id) {
        Product product = findProduct(id);
        List<String> imageUrls = product.getImages().stream().map(ProductImage::getUrl).toList();
        products.delete(product);
        products.flush();
        imageUrls.forEach(storage::delete);
    }

    private void applyProduct(Product product, ProductRequest r) {
        product.setCategory(r.categoryId() == null ? null : findCategory(r.categoryId()));
        product.setBrand(blankToNull(r.brand()));
        product.setFlavor(blankToNull(r.flavor()));
        product.setUnitVolumeMl(r.unitVolumeMl());
        product.setUnitsPerCase(r.unitsPerCase());
        product.setWeight(blankToNull(r.weight()));
        product.setPrice(r.price());
        product.setOriginalPrice(r.originalPrice());
        product.setCostPrice(r.costPrice());
        product.setLowStockThreshold(Objects.requireNonNullElse(r.lowStockThreshold(), product.getLowStockThreshold()));
        product.setBarcode(blankToNull(r.barcode()));
        product.setBadge(blankToNull(r.badge()));
        product.setDescription(blankToNull(r.description()));
        product.setIngredients(blankToNull(r.ingredients()));
        if (r.active() != null) {
            product.setActive(r.active());
        }
    }

    /** Replaces all bulk price slabs of a product. */
    public AdminProductResponse replaceSlabs(UUID id, List<SlabRequest> requested) {
        Product product = findProduct(id);
        List<SlabRequest> slabs = validateSlabs(requested, product.getPrice());
        product.getSlabs().clear();
        // Delete the old rows before inserting, or (product_id, min_qty) would collide.
        entityManager.flush();
        slabs.forEach(s -> product.addSlab(s.minQty(), s.maxQty(), s.pricePerCase()));
        return mapper.toAdmin(product);
    }

    /** Sorts slabs by min qty and rejects duplicates, overlaps and prices above the base price. */
    public static List<SlabRequest> validateSlabs(List<SlabRequest> slabs, BigDecimal basePrice) {
        List<SlabRequest> sorted = slabs.stream().sorted(Comparator.comparingInt(SlabRequest::minQty)).toList();
        for (int i = 0; i < sorted.size(); i++) {
            SlabRequest slab = sorted.get(i);
            if (slab.maxQty() != null && slab.maxQty() < slab.minQty()) {
                throw new ApiException(ErrorCode.INVALID_PRICE_SLABS, "Max cases must be at least min cases (" + slab.minQty() + ")");
            }
            if (slab.pricePerCase().compareTo(basePrice) > 0) {
                throw new ApiException(ErrorCode.INVALID_PRICE_SLABS, "Slab price for " + slab.minQty() + "+ cases is above the base price");
            }
            if (i > 0) {
                SlabRequest previous = sorted.get(i - 1);
                if (previous.minQty().equals(slab.minQty())) {
                    throw new ApiException(ErrorCode.INVALID_PRICE_SLABS, "Two slabs start at " + slab.minQty() + " cases");
                }
                if (previous.maxQty() != null && slab.minQty() <= previous.maxQty()) {
                    throw new ApiException(ErrorCode.INVALID_PRICE_SLABS, "Slabs " + previous.minQty() + "-" + previous.maxQty()
                            + " and " + slab.minQty() + "+ overlap");
                }
            }
        }
        return sorted;
    }

    public AdminProductResponse addImage(UUID productId, MultipartFile file) {
        Product product = findProduct(productId);
        String url = storage.storeImage(file, "products");
        product.addImage(url);
        // Insert now so the new image has its id in the response.
        entityManager.flush();
        return mapper.toAdmin(product);
    }

    public AdminProductResponse makePrimaryImage(UUID productId, UUID imageId) {
        Product product = findProduct(productId);
        product.makePrimary(findImage(product, imageId));
        return mapper.toAdmin(product);
    }

    public AdminProductResponse deleteImage(UUID productId, UUID imageId) {
        Product product = findProduct(productId);
        ProductImage image = findImage(product, imageId);
        product.removeImage(image);
        entityManager.flush();
        storage.delete(image.getUrl());
        return mapper.toAdmin(product);
    }

    private Product findProduct(UUID id) {
        return products.findById(id).orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found"));
    }

    private static ProductImage findImage(Product product, UUID imageId) {
        return product.getImages().stream().filter(i -> i.getId().equals(imageId)).findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.IMAGE_NOT_FOUND, "Image not found"));
    }

    // ---- Banners ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BannerResponse> listBanners() {
        return banners.findAllByOrderBySortOrderAsc().stream().map(catalogMapper::toBanner).toList();
    }

    public BannerResponse createBanner(BannerRequest request) {
        Banner banner = new Banner(request.title().trim());
        applyBanner(banner, request);
        banner.setSortOrder((int) banners.count());
        return catalogMapper.toBanner(banners.save(banner));
    }

    public BannerResponse updateBanner(UUID id, BannerRequest request) {
        Banner banner = findBanner(id);
        banner.setTitle(request.title().trim());
        applyBanner(banner, request);
        return catalogMapper.toBanner(banner);
    }

    public BannerResponse uploadBannerImage(UUID id, MultipartFile file) {
        Banner banner = findBanner(id);
        String previous = banner.getImageUrl();
        banner.setImageUrl(storage.storeImage(file, "banners"));
        storage.delete(previous);
        return catalogMapper.toBanner(banner);
    }

    public void deleteBanner(UUID id) {
        Banner banner = findBanner(id);
        banners.delete(banner);
        banners.flush();
        storage.delete(banner.getImageUrl());
    }

    /** {@code ids} must list every banner exactly once, in the new order. */
    public List<BannerResponse> reorderBanners(List<UUID> ids) {
        Map<UUID, Banner> all = banners.findAll().stream().collect(Collectors.toMap(Banner::getId, Function.identity()));
        if (ids.size() != all.size() || !new HashSet<>(ids).equals(all.keySet())) {
            throw new ApiException(ErrorCode.INVALID_BANNER_ORDER, "Send every banner id exactly once");
        }
        for (int i = 0; i < ids.size(); i++) {
            all.get(ids.get(i)).setSortOrder(i);
        }
        return listBanners();
    }

    private static void applyBanner(Banner banner, BannerRequest r) {
        banner.setSubtitle(blankToNull(r.subtitle()));
        banner.setCtaText(blankToNull(r.ctaText()));
        banner.setLink(blankToNull(r.link()));
        if (r.imageUrl() != null) {
            banner.setImageUrl(blankToNull(r.imageUrl()));
        }
        if (r.active() != null) {
            banner.setActive(r.active());
        }
    }

    private Banner findBanner(UUID id) {
        return banners.findById(id).orElseThrow(() -> new ApiException(ErrorCode.BANNER_NOT_FOUND, "Banner not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
