package com.smartretail.settings;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class SettingsAndShopProfileApiTest {

    @Autowired
    MockMvc mvc;

    String admin;
    String original;

    @BeforeEach
    void rememberSettings() throws Exception {
        admin = TestAuth.adminBearer(mvc);
        String settings = mvc.perform(get("/api/admin/settings").header("Authorization", admin))
                .andReturn().getResponse().getContentAsString();
        // Keep only the settings themselves, so they can be sent back unchanged afterwards.
        original = JsonPath.parse((Object) JsonPath.read(settings, "$.data")).jsonString();
    }

    /** Other tests rely on the seed values (delivery ₹500, minimum ₹1,000). */
    @AfterEach
    void restoreSettings() throws Exception {
        json(put("/api/admin/settings"), admin, original).andExpect(status().isOk());
    }

    @Test
    void adminReadsTheSeedSettings() throws Exception {
        mvc.perform(get("/api/admin/settings").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("S'Mart Retail"))
                .andExpect(jsonPath("$.data.storeOpen").value(true))
                .andExpect(jsonPath("$.data.deliveryCharge").value(500))
                .andExpect(jsonPath("$.data.freeDeliveryAbove").value(5000))
                .andExpect(jsonPath("$.data.minOrderValue").value(1000))
                .andExpect(jsonPath("$.data.upiId").value("smartretail@upi"));
        mvc.perform(get("/api/admin/settings").header("Authorization", TestAuth.customerBearer(mvc)))
                .andExpect(status().isForbidden());
    }

    @Test
    void savedSettingsReachTheStorefrontAndCheckoutAtOnce() throws Exception {
        json(put("/api/admin/settings"), admin, """
                {"storeName": " Sri Sai Beverages ", "storePhone": "9876500000", "storeEmail": "Orders@SriSai.in",
                 "storeAddress": "Warangal, Telangana", "whatsappNumber": "", "storeOpen": false,
                 "deliveryCharge": 300, "freeDeliveryAbove": 8000, "minOrderValue": 2500, "upiId": "srisai@okaxis",
                 "gstNumber": "36abcde1234f1z5", "fssaiNumber": "13624002000232", "showCertifications": true}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("Sri Sai Beverages"))
                .andExpect(jsonPath("$.data.storeEmail").value("orders@srisai.in"))
                .andExpect(jsonPath("$.data.gstNumber").value("36ABCDE1234F1Z5"));

        mvc.perform(get("/api/settings/public"))
                .andExpect(jsonPath("$.data.store_name").value("Sri Sai Beverages"))
                .andExpect(jsonPath("$.data.store_open").value("false"))
                .andExpect(jsonPath("$.data.delivery_charge").value("300"))
                .andExpect(jsonPath("$.data.gst_number").value("36ABCDE1234F1Z5"));

        String productId = JsonPath.read(mvc.perform(get("/api/products/bisleri-1l-packaged-water"))
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        json(post("/api/checkout/quote"), null, "{\"items\": [{\"productId\": \"" + productId + "\", \"quantity\": 5}]}")
                .andExpect(jsonPath("$.data.storeOpen").value(false))
                .andExpect(jsonPath("$.data.deliveryCharge").value(300.0))
                .andExpect(jsonPath("$.data.minOrderValue").value(2500))
                .andExpect(jsonPath("$.data.upiId").value("srisai@okaxis"))
                .andExpect(jsonPath("$.data.problems", hasSize(2)))
                .andExpect(jsonPath("$.data.problems[0]").value(org.hamcrest.Matchers.startsWith("The store is closed")))
                .andExpect(jsonPath("$.data.problems[1]").value("Minimum order is ₹2,500. Add ₹1,300 more."));
    }

    @Test
    void rejectsInvalidSettings() throws Exception {
        json(put("/api/admin/settings"), admin, """
                {"storeName": "", "storeOpen": null, "deliveryCharge": -5, "freeDeliveryAbove": 0, "minOrderValue": 0,
                 "upiId": "not-a-upi", "gstNumber": "12345", "fssaiNumber": "abc", "storeEmail": "nope",
                 "showCertifications": true}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.storeName").exists())
                .andExpect(jsonPath("$.error.details.storeOpen").exists())
                .andExpect(jsonPath("$.error.details.deliveryCharge").exists())
                .andExpect(jsonPath("$.error.details.upiId").value("must look like name@bank"))
                .andExpect(jsonPath("$.error.details.gstNumber").value("must be a valid 15-character GSTIN"))
                .andExpect(jsonPath("$.error.details.fssaiNumber").value("must be the 14-digit FSSAI number"))
                .andExpect(jsonPath("$.error.details.storeEmail").exists());
    }

    @Test
    void shopOwnerCompletesTheShopProfileWithLocation() throws Exception {
        String customer = TestAuth.customerBearer(mvc);
        mvc.perform(get("/api/me/shop-profile").header("Authorization", customer))
                .andExpect(jsonPath("$.data.complete").value(false))
                .andExpect(jsonPath("$.data.ownerName").value("Test Shop"));

        json(put("/api/me/shop-profile"), customer, """
                {"shopName": "Lakshmi Kirana", "ownerName": "Ravi Kumar", "phone": "+91 98765 11111",
                 "gstNumber": "36abcde1234f1z5", "address": "4-1-20, Main Road, Ameerpet", "city": "Hyderabad",
                 "pincode": "500016", "lat": 17.4374614, "lng": 78.4482878}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.complete").value(true))
                .andExpect(jsonPath("$.data.phone").value("9876511111"))
                .andExpect(jsonPath("$.data.gstNumber").value("36ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.data.state").value("Telangana"))
                .andExpect(jsonPath("$.data.lat").value(17.437461));

        mvc.perform(get("/api/me").header("Authorization", customer))
                .andExpect(jsonPath("$.data.shopProfileComplete").value(true));
        // The shop becomes the default delivery address, so checkout is ready.
        mvc.perform(get("/api/me/addresses").header("Authorization", customer))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].label").value("Shop"))
                .andExpect(jsonPath("$.data[0].addressLine1").value("4-1-20, Main Road, Ameerpet"))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));

        // Saving again updates the profile but does not add a second address.
        json(put("/api/me/shop-profile"), customer, """
                {"shopName": "Lakshmi Kirana & General", "ownerName": "Ravi Kumar", "phone": "9876511111",
                 "address": "4-1-20, Main Road, Ameerpet", "city": "Hyderabad", "pincode": "500016",
                 "lat": 17.44, "lng": 78.45}
                """)
                .andExpect(jsonPath("$.data.shopName").value("Lakshmi Kirana & General"))
                .andExpect(jsonPath("$.data.gstNumber").doesNotExist());
        mvc.perform(get("/api/me/addresses").header("Authorization", customer)).andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void rejectsBadShopProfiles() throws Exception {
        String customer = TestAuth.customerBearer(mvc);
        json(put("/api/me/shop-profile"), customer, """
                {"shopName": "", "ownerName": "Ravi", "phone": "9876511111", "address": "x", "city": "y",
                 "pincode": "5000", "lat": 78.4, "lng": 17.4, "gstNumber": "BAD"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.shopName").exists())
                .andExpect(jsonPath("$.error.details.pincode").value("must be a 6-digit PIN code"))
                .andExpect(jsonPath("$.error.details.lat").exists())
                .andExpect(jsonPath("$.error.details.lng").exists())
                .andExpect(jsonPath("$.error.details.gstNumber").exists());
        json(put("/api/me/shop-profile"), null, "{}").andExpect(status().isUnauthorized());
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String bearer, String body) throws Exception {
        if (bearer != null) {
            request.header("Authorization", bearer);
        }
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
