package com.smartretail.picklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class PickListAndCustomersApiTest {

    /** Adilabad, used by no other test. */
    private static final String LAT = "19.6641";
    private static final String LNG = "78.5320";

    @Autowired
    MockMvc mvc;

    String admin;
    String route;
    String routeName;

    @BeforeEach
    void setUp() throws Exception {
        admin = TestAuth.adminBearer(mvc);
        // Routes with orders stay in the database, so each test needs its own name.
        routeName = "Adilabad " + UUID.randomUUID();
        route = JsonPath.read(json(post("/api/admin/routes"), admin, """
                {"name": "%s", "days": ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"],
                 "zone": {"shape": "CIRCLE", "center": {"lat": %s, "lng": %s}, "radiusKm": 5}}
                """.formatted(routeName, LAT, LNG)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    /** Routes with orders cannot be deleted, so this one is turned off for the other tests. */
    @AfterEach
    void turnRouteOff() throws Exception {
        json(patch("/api/admin/routes/" + route + "/active"), admin, "{\"active\": false}").andExpect(status().isOk());
    }

    @Test
    void pickListAddsUpARoutesOrdersForTheDeliveryDate() throws Exception {
        String thumsUp = productId("thums-up-750ml-pet");
        String bisleri = productId("bisleri-1l-packaged-water");
        int unitsPerCase = JsonPath.read(mvc.perform(get("/api/products/thums-up-750ml-pet"))
                .andReturn().getResponse().getContentAsString(), "$.unitsPerCase");

        String first = shopOnRoute("Lakshmi Kirana");
        String second = shopOnRoute("Balaji Stores");
        String firstOrder = place(first, items(thumsUp, 5, bisleri, 2));
        place(second, items(thumsUp, 3));
        String cancelled = place(second, items(bisleri, 10));
        json(post("/api/admin/orders/" + cancelled + "/status"), admin, "{\"status\":\"CANCELLED\"}")
                .andExpect(status().isOk());

        String date = JsonPath.read(mvc.perform(get("/api/me/orders/" + firstOrder).header("Authorization", first))
                .andReturn().getResponse().getContentAsString(), "$.deliveryDate");
        assertThat(LocalDate.parse(date)).isAfter(LocalDate.now().minusDays(1));

        mvc.perform(get("/api/admin/pick-list").header("Authorization", admin)
                        .param("route", route).param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeName").value(routeName))
                .andExpect(jsonPath("$.orders", hasSize(2)))
                .andExpect(jsonPath("$.orders[*].shopName", contains("Balaji Stores", "Lakshmi Kirana")))
                .andExpect(jsonPath("$.totals.orders").value(2))
                .andExpect(jsonPath("$.totals.uniqueSkus").value(2))
                .andExpect(jsonPath("$.totals.cases").value(10))
                .andExpect(jsonPath("$.items[?(@.productId == '%s')].cases".formatted(thumsUp), contains(8)))
                .andExpect(jsonPath("$.items[?(@.productId == '%s')].units".formatted(thumsUp),
                        contains(8 * unitsPerCase)))
                .andExpect(jsonPath("$.items[?(@.productId == '%s')].cases".formatted(bisleri), contains(2)));

        // Another date has nothing to load.
        mvc.perform(get("/api/admin/pick-list").header("Authorization", admin)
                        .param("route", route).param("date", LocalDate.parse(date).plusDays(30).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totals.cases").value(0));
    }

    @Test
    void pickListNeedsAKnownRoute() throws Exception {
        mvc.perform(get("/api/admin/pick-list").header("Authorization", admin)
                        .param("route", "all").param("date", "2026-11-02"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/pick-list").header("Authorization", admin)
                        .param("route", "00000000-0000-0000-0000-000000000001").param("date", "2026-11-02"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/pick-list").header("Authorization", admin)
                        .param("route", "unassigned").param("date", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeName").value("No route"));
        mvc.perform(get("/api/admin/pick-list").header("Authorization", TestAuth.customerBearer(mvc))
                        .param("route", route).param("date", "2026-11-02"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customersListShowsRoutesAndFilters() throws Exception {
        shopOnRoute("Directory Kirana");
        String noPin = TestAuth.customerBearer(mvc);
        String noPinPhone = JsonPath.read(mvc.perform(get("/api/me").header("Authorization", noPin))
                .andReturn().getResponse().getContentAsString(), "$.phone");

        mvc.perform(get("/api/admin/customers").header("Authorization", admin).param("q", "directory kirana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].shopName").value("Directory Kirana"))
                .andExpect(jsonPath("$.items[0].routeName").value(routeName))
                .andExpect(jsonPath("$.items[0].hasLocation").value(true))
                .andExpect(jsonPath("$.items[0].joinedAt").isNotEmpty());

        mvc.perform(get("/api/admin/customers").header("Authorization", admin).param("route", route).param("size", "100"))
                .andExpect(jsonPath("$.items[*].shopName", hasItem("Directory Kirana")))
                .andExpect(jsonPath("$.items[*].phone", not(hasItem(noPinPhone))));
        mvc.perform(get("/api/admin/customers").header("Authorization", admin)
                        .param("route", "unassigned").param("q", noPinPhone))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].hasLocation").value(false));

        mvc.perform(get("/api/admin/customers").header("Authorization", admin).param("route", "nonsense"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/customers").header("Authorization", admin).param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    private String shopOnRoute(String shopName) throws Exception {
        String bearer = TestAuth.customerBearer(mvc);
        json(put("/api/me/shop-profile"), bearer, """
                {"shopName": "%s", "ownerName": "Ravi", "phone": "9876501234", "address": "1 Main Road",
                 "city": "Adilabad", "pincode": "504001", "lat": %s, "lng": %s}
                """.formatted(shopName, LAT, LNG)).andExpect(status().isOk());
        return bearer;
    }

    /** Places a cash order at the quoted total, on the address the shop profile created. */
    private String place(String bearer, String items) throws Exception {
        String addressId = JsonPath.read(mvc.perform(get("/api/me/addresses").header("Authorization", bearer))
                .andReturn().getResponse().getContentAsString(), "$[0].id");
        Number total = JsonPath.read(json(post("/api/checkout/quote"), bearer, "{\"items\":" + items + "}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.total");
        return JsonPath.read(json(post("/api/orders"), bearer, """
                {"items": %s, "addressId": "%s", "paymentMethod": "COD", "expectedTotal": %s}
                """.formatted(items, addressId, total)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String productId(String slug) throws Exception {
        return JsonPath.read(mvc.perform(get("/api/products/" + slug)).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static String items(Object... idsAndQuantities) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < idsAndQuantities.length; i += 2) {
            json.append(i > 0 ? "," : "").append("{\"productId\":\"").append(idsAndQuantities[i])
                    .append("\",\"quantity\":").append(idsAndQuantities[i + 1]).append('}');
        }
        return json.append(']').toString();
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String bearer, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
