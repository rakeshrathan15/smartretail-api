package com.smartretail.catalog;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartretail.IntegrationTest;
import com.smartretail.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** Public storefront endpoints, checked against the dev seed catalog. No login used. */
@IntegrationTest
class CatalogApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listsActiveCategoriesInOrder() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("colas"))
                .andExpect(jsonPath("$.data[*].slug", hasItem("sparkling-lime")));
    }

    @Test
    void filtersProductsByCategoryAndShowsSlabStartingPrice() throws Exception {
        mvc.perform(get("/api/products").param("category", "colas").param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.items[*].slug",
                        contains("thums-up-750ml-pet", "coca-cola-300ml-bottle", "diet-coke-330ml-can")))
                .andExpect(jsonPath("$.data.items[0].price").value(420.0))
                .andExpect(jsonPath("$.data.items[0].lowestPrice").value(357.0))
                .andExpect(jsonPath("$.data.items[0].unitsPerCase").value(12))
                .andExpect(jsonPath("$.data.items[0].inStock").value(true));
    }

    @Test
    void searchesNameBrandAndFlavor() throws Exception {
        mvc.perform(get("/api/products").param("q", "MANGO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].slug", containsInAnyOrder("maaza-mango-600ml", "slice-aam-panna-250ml")));
    }

    @Test
    void dealsAreProductsBelowTheirOriginalPrice() throws Exception {
        mvc.perform(get("/api/products").param("deals", "true").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()", greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.data.items[*].originalPrice", not(hasItem((Object) null))))
                .andExpect(jsonPath("$.data.items[*].slug", not(hasItem("bisleri-1l-packaged-water"))));
    }

    @Test
    void paginatesAndCapsPageSize() throws Exception {
        mvc.perform(get("/api/products").param("size", "4").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(4))
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.totalItems", greaterThanOrEqualTo(15)));
        mvc.perform(get("/api/products").param("size", "1000"))
                .andExpect(jsonPath("$.data.size").value(CatalogService.MAX_PAGE_SIZE));
        mvc.perform(get("/api/products").param("sort", "cheapest")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void showsProductDetailWithSlabsAndCategory() throws Exception {
        mvc.perform(get("/api/products/maaza-mango-600ml"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Maaza Mango 600ml"))
                .andExpect(jsonPath("$.data.category.slug").value("juices"))
                .andExpect(jsonPath("$.data.slabs[*].minQty", contains(10, 25)))
                .andExpect(jsonPath("$.data.slabs[0].pricePerCase").value(810.0))
                .andExpect(jsonPath("$.data.lowestPrice").value(765.0))
                .andExpect(jsonPath("$.data.images").isEmpty())
                .andExpect(jsonPath("$.data.costPrice").doesNotExist());
        mvc.perform(get("/api/products/no-such-product")).andExpect(status().isNotFound());
    }

    @Test
    void outOfStockProductsAreListedButMarked() throws Exception {
        mvc.perform(get("/api/products/aquafina-200ml-mini"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inStock").value(false));
    }

    @Test
    void listsBannersAndPublicSettings() throws Exception {
        mvc.perform(get("/api/banners"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].active", everyItem(org.hamcrest.Matchers.is(true))));
        mvc.perform(get("/api/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.store_name").value("S'Mart Retail"))
                .andExpect(jsonPath("$.data.store_open").value("true"))
                .andExpect(jsonPath("$.data.min_order_value").value("1000"));
    }
}
