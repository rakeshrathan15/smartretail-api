package com.smartretail.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class AdminCatalogApiTest {

    // Smallest valid PNG header bytes; enough for the type check.
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired
    MockMvc mvc;

    String admin;

    @BeforeEach
    void signIn() throws Exception {
        admin = TestAuth.adminBearer(mvc);
    }

    @Test
    void customersCannotManageTheCatalog() throws Exception {
        String customer = TestAuth.customerBearer(mvc);
        mvc.perform(get("/api/admin/products").header("Authorization", customer)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/categories").header("Authorization", customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hack\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void createsProductWithSlabsAndControlsVisibility() throws Exception {
        String categoryId = createCategory("Test Sodas " + UUID.randomUUID().toString().substring(0, 6));

        String body = perform(post("/api/admin/products"), """
                {"name": "Test Soda 500ml", "categoryId": "%s", "brand": "Testo", "unitVolumeMl": 500,
                 "unitsPerCase": 24, "price": 600, "originalPrice": 650, "costPrice": 520, "stockQuantity": 40}
                """.formatted(categoryId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.slug").value(startsWith("test-soda-500ml")))
                .andExpect(jsonPath("$.costPrice").value(520.0))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.product.id");
        String slug = JsonPath.read(body, "$.product.slug");

        perform(put("/api/admin/products/" + id + "/slabs"), """
                {"slabs": [{"minQty": 20, "pricePerCase": 540}, {"minQty": 5, "maxQty": 19, "pricePerCase": 570}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.slabs[*].minQty", contains(5, 20)))
                .andExpect(jsonPath("$.product.lowestPrice").value(540.0));
        // Replacing slabs again reuses min qty 5, which must not collide with the old row.
        perform(put("/api/admin/products/" + id + "/slabs"), """
                {"slabs": [{"minQty": 5, "pricePerCase": 580}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.slabs.length()").value(1));
        perform(put("/api/admin/products/" + id + "/slabs"), """
                {"slabs": [{"minQty": 5, "maxQty": 30, "pricePerCase": 580}, {"minQty": 20, "pricePerCase": 560}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("overlap")));

        mvc.perform(get("/api/products/" + slug)).andExpect(status().isOk());

        perform(put("/api/admin/products/" + id), """
                {"name": "Test Soda 500ml", "categoryId": "%s", "unitsPerCase": 24, "price": 600, "active": false}
                """.formatted(categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/products/" + slug)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/products").header("Authorization", admin).param("q", "test soda"))
                .andExpect(jsonPath("$.items[0].slug").value(slug))
                .andExpect(jsonPath("$.items[0].active").value(false))
                .andExpect(jsonPath("$.items[0].stockQuantity").value(40))
                .andExpect(jsonPath("$.items[0].categoryName").value(startsWith("Test Sodas")));

        // A category with products cannot be deleted.
        mvc.perform(delete("/api/admin/categories/" + categoryId).header("Authorization", admin))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/products/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/categories/" + categoryId).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void validatesProductAndCategoryInput() throws Exception {
        perform(post("/api/admin/products"), "{\"name\": \"\", \"unitsPerCase\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.unitsPerCase").exists())
                .andExpect(jsonPath("$.errors.price").exists());
        perform(post("/api/admin/categories"), "{\"name\": \"colas\"}").andExpect(status().isConflict());
        perform(post("/api/admin/products"), """
                {"name": "Orphan", "categoryId": "%s", "unitsPerCase": 1, "price": 10}
                """.formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadsServesAndRemovesProductImages() throws Exception {
        String body = perform(post("/api/admin/products"), """
                {"name": "Image Test Drink", "unitsPerCase": 6, "price": 100}
                """).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.product.id");

        String first = upload("/api/admin/products/" + id + "/images", "front.png", PNG)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.images[0].primary").value(true))
                .andExpect(jsonPath("$.product.images[0].url").value(startsWith("/uploads/products/")))
                .andReturn().getResponse().getContentAsString();
        String firstUrl = JsonPath.read(first, "$.product.images[0].url");
        String firstId = JsonPath.read(first, "$.product.images[0].id");

        mvc.perform(get(firstUrl)).andExpect(status().isOk()).andExpect(content().bytes(PNG));

        String second = upload("/api/admin/products/" + id + "/images", "back.png", PNG)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.images[1].primary").value(false))
                .andReturn().getResponse().getContentAsString();
        String secondId = JsonPath.read(second, "$.product.images[1].id");

        mvc.perform(put("/api/admin/products/" + id + "/images/" + secondId + "/primary").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.images[?(@.primary == true)].id", contains(secondId)));

        mvc.perform(get("/api/admin/products").header("Authorization", admin).param("q", "image test drink"))
                .andExpect(jsonPath("$.items[0].imageUrl").value(JsonPath.<String>read(second, "$.product.images[1].url")));

        mvc.perform(delete("/api/admin/products/" + id + "/images/" + firstId).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.images.length()").value(1));
        mvc.perform(get(firstUrl)).andExpect(status().isNotFound());
        assertThat(Files.exists(Path.of("build/test-uploads", firstUrl.substring("/uploads/".length())))).isFalse();

        upload("/api/admin/products/" + id + "/images", "evil.png", "<script>alert(1)</script>".getBytes())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only JPG, PNG or WebP images are allowed"));

        mvc.perform(delete("/api/admin/products/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
    }

    @Test
    void managesAndReordersBanners() throws Exception {
        String created = perform(post("/api/admin/banners"), """
                {"title": "Diwali offer", "subtitle": "Extra 5% on 50+ cases", "ctaText": "Shop", "link": "/shop"}
                """).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String bannerId = JsonPath.read(created, "$.id");

        upload("/api/admin/banners/" + bannerId + "/image", "banner.png", PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(startsWith("/uploads/banners/")));

        String all = mvc.perform(get("/api/admin/banners").header("Authorization", admin))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(all, "$[*].id");
        List<String> reversed = ids.reversed();
        perform(put("/api/admin/banners/order"), "{\"ids\": [\"" + String.join("\",\"", reversed) + "\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reversed.getFirst()));
        perform(put("/api/admin/banners/order"), "{\"ids\": [\"" + bannerId + "\"]}")
                .andExpect(status().isBadRequest());

        perform(put("/api/admin/banners/" + bannerId), "{\"title\": \"Diwali offer\", \"active\": false}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/banners")).andExpect(jsonPath("$[*].id", not(hasItem(bannerId))));

        mvc.perform(delete("/api/admin/banners/" + bannerId).header("Authorization", admin))
                .andExpect(status().isNoContent());
        // Put the seed banners back in their original order for other tests.
        List<String> remaining = ids.stream().filter(i -> !i.equals(bannerId)).toList();
        perform(put("/api/admin/banners/order"), "{\"ids\": [\"" + String.join("\",\"", remaining) + "\"]}")
                .andExpect(status().isOk());
    }

    private String createCategory(String name) throws Exception {
        String body = perform(post("/api/admin/categories"), "{\"name\": \"" + name + "\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(request.header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions upload(String path, String fileName, byte[] bytes) throws Exception {
        return mvc.perform(multipart(path).file(new MockMultipartFile("file", fileName, "image/png", bytes))
                .header("Authorization", admin));
    }
}
