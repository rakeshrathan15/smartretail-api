package com.smartretail.route;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Each test works in its own town, so routes never catch another test's shops. */
@IntegrationTest
class DeliveryRouteApiTest {

    private static final String WARANGAL = "\"lat\": 17.9689, \"lng\": 79.5941";

    @Autowired
    MockMvc mvc;

    String admin;
    final List<String> created = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        admin = TestAuth.adminBearer(mvc);
    }

    @AfterEach
    void removeRoutes() throws Exception {
        for (String id : created) {
            mvc.perform(delete("/api/admin/routes/" + id).header("Authorization", admin));
        }
    }

    @Test
    void aShopJoinsTheRouteAroundItsPin() throws Exception {
        String route = createRoute("""
                {"name": "Warangal Test", "area": "Hanamkonda", "days": ["THU", "MON"], "color": "#2563eb",
                 "zone": {"shape": "CIRCLE", "center": {%s}, "radiusKm": 3}}
                """.formatted(WARANGAL));

        String inside = TestAuth.customerBearer(mvc);
        saveShop(inside, "17.9700", "79.5950");
        mvc.perform(get("/api/me/delivery-route").header("Authorization", inside))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(true))
                .andExpect(jsonPath("$.name").value("Warangal Test"))
                .andExpect(jsonPath("$.days", contains("MON", "THU")))
                .andExpect(jsonPath("$.nextDeliveryDate").isNotEmpty());

        String outside = TestAuth.customerBearer(mvc);
        saveShop(outside, "18.4386", "79.1288");
        mvc.perform(get("/api/me/delivery-route").header("Authorization", outside))
                .andExpect(jsonPath("$.assigned").value(false))
                .andExpect(jsonPath("$.name", nullValue()));

        mvc.perform(get("/api/admin/routes").header("Authorization", admin))
                .andExpect(jsonPath("$[?(@.id == '%s')].shopCount".formatted(route), contains(1)))
                .andExpect(jsonPath("$[?(@.id == '%s')].color".formatted(route), contains("#2563EB")));
        mvc.perform(get("/api/admin/routes/shop-pins").header("Authorization", admin))
                .andExpect(jsonPath("$[*].routeId", hasItem(route)));
    }

    @Test
    void shopsMoveWhenARouteChangesAndLeaveWhenItIsTurnedOff() throws Exception {
        String shop = TestAuth.customerBearer(mvc);
        saveShop(shop, "18.6725", "78.0941");
        mvc.perform(get("/api/me/delivery-route").header("Authorization", shop))
                .andExpect(jsonPath("$.assigned").value(false));

        // A square drawn over Nizamabad picks up the shop that was already there.
        String body = """
                {"name": "Nizamabad Test", "days": [], "zone": {"shape": "POLYGON", "points": [
                 {"lat": 18.65, "lng": 78.07}, {"lat": 18.65, "lng": 78.12},
                 {"lat": 18.70, "lng": 78.12}, {"lat": 18.70, "lng": 78.07}]}}
                """;
        String route = createRoute(body);
        mvc.perform(get("/api/me/delivery-route").header("Authorization", shop))
                .andExpect(jsonPath("$.name").value("Nizamabad Test"));

        json(patch("/api/admin/routes/" + route + "/active"), "{\"active\": false}").andExpect(status().isOk());
        mvc.perform(get("/api/me/delivery-route").header("Authorization", shop))
                .andExpect(jsonPath("$.assigned").value(false));

        json(patch("/api/admin/routes/" + route + "/active"), "{\"active\": true}").andExpect(status().isOk());
        mvc.perform(get("/api/me/delivery-route").header("Authorization", shop))
                .andExpect(jsonPath("$.assigned").value(true));

        // Moving the area away takes the shop off.
        json(put("/api/admin/routes/" + route), """
                {"name": "Nizamabad Test", "days": [], "zone": {"shape": "CIRCLE",
                 "center": {"lat": 18.90, "lng": 78.30}, "radiusKm": 2}}
                """).andExpect(status().isOk()).andExpect(jsonPath("$.zone.shape").value("CIRCLE"));
        mvc.perform(get("/api/me/delivery-route").header("Authorization", shop))
                .andExpect(jsonPath("$.assigned").value(false));
    }

    @Test
    void whereAreasOverlapTheLowerSortOrderWins() throws Exception {
        String shop = TestAuth.customerBearer(mvc);
        saveShop(shop, "17.2473", "80.1514");
        String center = "\"center\": {\"lat\": 17.2473, \"lng\": 80.1514}";
        createRoute("""
                {"name": "Khammam Second", "days": [], "sortOrder": 20,
                 "zone": {"shape": "CIRCLE", %s, "radiusKm": 5}}
                """.formatted(center));
        createRoute("""
                {"name": "Khammam First", "days": [], "sortOrder": 10,
                 "zone": {"shape": "CIRCLE", %s, "radiusKm": 5}}
                """.formatted(center));
        mvc.perform(get("/api/me/delivery-route").header("Authorization", shop))
                .andExpect(jsonPath("$.name").value("Khammam First"));
    }

    @Test
    void checksRouteDetails() throws Exception {
        json(post("/api/admin/routes"), "{\"name\": \"\", \"days\": [\"MONDAY\"], \"color\": \"blue\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.color").value("must be a colour like #2563EB"));
        json(post("/api/admin/routes"), """
                {"name": "Too few points", "days": [], "zone": {"shape": "POLYGON",
                 "points": [{"lat": 17.1, "lng": 78.1}, {"lat": 17.2, "lng": 78.2}]}}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("An area needs 3 to 200 points"));
        json(post("/api/admin/routes"), """
                {"name": "Huge circle", "days": [], "zone": {"shape": "CIRCLE",
                 "center": {"lat": 17.1, "lng": 78.1}, "radiusKm": 500}}
                """).andExpect(status().isBadRequest());
        json(post("/api/admin/routes"), """
                {"name": "Not in India", "days": [], "zone": {"shape": "CIRCLE",
                 "center": {"lat": 78.1, "lng": 17.1}, "radiusKm": 5}}
                """).andExpect(status().isBadRequest());
        json(post("/api/admin/routes"), """
                {"name": "No such store", "days": [], "storeId": "00000000-0000-0000-0000-000000000001"}
                """).andExpect(status().isNotFound());
        createRoute("{\"name\": \"Only once\", \"days\": []}");
        json(post("/api/admin/routes"), "{\"name\": \"ONLY ONCE\", \"days\": []}").andExpect(status().isConflict());
        mvc.perform(get("/api/admin/routes").header("Authorization", TestAuth.customerBearer(mvc)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reassignReportsWhereShopsLanded() throws Exception {
        mvc.perform(post("/api/admin/routes/reassign").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").isNumber())
                .andExpect(jsonPath("$.unassigned").isNumber());
    }

    private String createRoute(String body) throws Exception {
        String id = JsonPath.read(json(post("/api/admin/routes"), body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        created.add(id);
        return id;
    }

    private void saveShop(String bearer, String lat, String lng) throws Exception {
        mvc.perform(put("/api/me/shop-profile").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"shopName": "Route Test Kirana", "ownerName": "Ravi", "phone": "9876501234",
                                 "address": "1 Main Road", "city": "Town", "pincode": "500001",
                                 "lat": %s, "lng": %s}
                                """.formatted(lat, lng)))
                .andExpect(status().isOk());
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
