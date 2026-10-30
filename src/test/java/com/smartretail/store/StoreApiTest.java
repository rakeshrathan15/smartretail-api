package com.smartretail.store;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class StoreApiTest {

    @Autowired
    MockMvc mvc;

    String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = TestAuth.adminBearer(mvc);
    }

    @Test
    void adminsAddEditAndRemoveStores() throws Exception {
        String id = JsonPath.read(json(post("/api/admin/stores"), """
                {"name": " Kukatpally Depot ", "gstin": "36abcde1234f1z5", "phone": "040 1234 5678",
                 "email": "Depot@Example.com", "city": "Hyderabad", "pincode": "500072"}
                """).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kukatpally Depot"))
                .andExpect(jsonPath("$.gstin").value("36ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.email").value("depot@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.routeCount").value(0))
                .andReturn().getResponse().getContentAsString(), "$.id");

        json(post("/api/admin/stores"), "{\"name\": \"kukatpally depot\"}").andExpect(status().isConflict());
        json(put("/api/admin/stores/" + id), "{\"name\": \"Kukatpally Depot\", \"address\": \"KPHB Phase 1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("KPHB Phase 1"))
                .andExpect(jsonPath("$.gstin").isEmpty());
        json(patch("/api/admin/stores/" + id + "/active"), "{\"active\": false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        // A store with a route cannot be deleted.
        String route = JsonPath.read(json(post("/api/admin/routes"), """
                {"name": "Depot test route", "days": ["MON"], "storeId": "%s"}
                """.formatted(id)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeName").value("Kukatpally Depot"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(delete("/api/admin/stores/" + id).header("Authorization", admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Kukatpally Depot has 1 delivery route; move them to another store first"));
        mvc.perform(delete("/api/admin/routes/" + route).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/stores/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
    }

    @Test
    void checksStoreDetails() throws Exception {
        json(post("/api/admin/stores"), "{\"name\": \"\", \"gstin\": \"BAD\", \"pincode\": \"5000\", \"email\": \"x\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.gstin").value("must be a valid 15-character GSTIN"))
                .andExpect(jsonPath("$.errors.pincode").value("must be a 6-digit PIN code"))
                .andExpect(jsonPath("$.errors.email").exists());
        mvc.perform(get("/api/admin/stores").header("Authorization", TestAuth.customerBearer(mvc)))
                .andExpect(status().isForbidden());
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
