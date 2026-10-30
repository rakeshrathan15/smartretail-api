package com.smartretail.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class AdminOrderApiTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    String admin;
    String customer;
    String address;
    String sprite;

    @BeforeEach
    void setUp() throws Exception {
        admin = TestAuth.adminBearer(mvc);
        customer = TestAuth.customerBearer(mvc);
        address = JsonPath.read(json(post("/api/me/addresses"), customer, """
                {"label": "Shop", "fullName": "Sri Lakshmi Stores", "phone": "9876512345", "addressLine1": "4 Market St",
                 "city": "Warangal", "pincode": "506002"}
                """).andReturn().getResponse().getContentAsString(), "$.id");
        sprite = JsonPath.read(mvc.perform(get("/api/products/sprite-300ml-bottle")).andReturn().getResponse()
                .getContentAsString(), "$.id");
    }

    @Test
    void listsFiltersAndSummarisesOrders() throws Exception {
        String orderNumber = JsonPath.read(placeCod(5, null), "$.orderNumber");

        mvc.perform(get("/api/admin/orders").header("Authorization", admin).param("q", orderNumber.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.items[0].customerName").value("Sri Lakshmi Stores"))
                .andExpect(jsonPath("$.items[0].customerPhone").value("9876512345"))
                .andExpect(jsonPath("$.items[0].totalCases").value(5));
        mvc.perform(get("/api/admin/orders").header("Authorization", admin).param("q", "9876512345")
                        .param("status", "PLACED").param("paymentMethod", "COD"))
                .andExpect(jsonPath("$.items[*].orderNumber", hasItem(orderNumber)))
                .andExpect(jsonPath("$.items[*].status", everyItem(is("PLACED"))));
        mvc.perform(get("/api/admin/orders").header("Authorization", admin).param("status", "DELIVERED")
                        .param("q", orderNumber))
                .andExpect(jsonPath("$.items", empty()));
        mvc.perform(get("/api/admin/orders").header("Authorization", admin).param("from", "2000-01-01")
                        .param("to", "2000-01-31"))
                .andExpect(jsonPath("$.totalItems").value(0));

        mvc.perform(get("/api/admin/orders/stats").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayOrders", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.awaitingConfirmation", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.codToCollect", greaterThanOrEqualTo(2900.0)));

        mvc.perform(get("/api/admin/orders").header("Authorization", customer)).andExpect(status().isForbidden());
    }

    @Test
    void movesAnOrderThroughDeliveryAndMarksCashPaid() throws Exception {
        String id = JsonPath.read(placeCod(5, null), "$.id");

        mvc.perform(get("/api/admin/orders/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.nextStatuses", contains("CONFIRMED", "CANCELLED")))
                .andExpect(jsonPath("$.order.items[0].productName").value("Sprite 300ml Bottle"))
                .andExpect(jsonPath("$.order.address.city").value("Warangal"))
                .andExpect(jsonPath("$.paymentNeedsReview").value(false));

        changeStatus(id, "DELIVERED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("An order that is placed cannot be marked delivered"));

        for (String next : new String[] {"CONFIRMED", "PACKED", "OUT_FOR_DELIVERY"}) {
            changeStatus(id, next).andExpect(status().isOk()).andExpect(jsonPath("$.order.status").value(next));
        }
        changeStatus(id, "DELIVERED")
                .andExpect(jsonPath("$.order.status").value("DELIVERED"))
                .andExpect(jsonPath("$.order.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.nextStatuses", contains("RETURNED")));

        // The shop sees the new status in its own order history.
        mvc.perform(get("/api/me/orders/" + id).header("Authorization", customer))
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void cancellingGivesBackStockAndTheCouponUse() throws Exception {
        int stockBefore = stock();
        int usesBefore = jdbc.queryForObject("select used_count from coupons where code = 'FLAT100'", Integer.class);

        String id = JsonPath.read(placeCod(5, "FLAT100"), "$.id");
        assertThat(stock()).isEqualTo(stockBefore - 5);
        assertThat(jdbc.queryForObject("select used_count from coupons where code = 'FLAT100'", Integer.class))
                .isEqualTo(usesBefore + 1);

        changeStatus(id, "CANCELLED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStatuses", empty()));
        assertThat(stock()).isEqualTo(stockBefore);
        assertThat(jdbc.queryForObject("select used_count from coupons where code = 'FLAT100'", Integer.class))
                .isEqualTo(usesBefore);

        // A second cancel must not add the stock back twice.
        changeStatus(id, "CANCELLED").andExpect(status().isConflict());
        assertThat(stock()).isEqualTo(stockBefore);
    }

    @Test
    void upiOrdersWaitForThePaymentCheck() throws Exception {
        String reference = JsonPath.read(mvc.perform(multipart("/api/me/payment-screenshots")
                        .file(new MockMultipartFile("file", "paid.png", "image/png", PNG))
                        .header("Authorization", customer))
                .andReturn().getResponse().getContentAsString(), "$.reference");
        String id = JsonPath.read(place("UPI", reference, 5, null), "$.id");

        // The screenshot is private: not on the public uploads path, only for admins.
        mvc.perform(get("/uploads/" + reference)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/orders/" + id + "/payment-screenshot").header("Authorization", customer))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/orders/" + id + "/payment-screenshot").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));

        mvc.perform(get("/api/admin/orders/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.hasPaymentScreenshot").value(true))
                .andExpect(jsonPath("$.paymentNeedsReview").value(true));
        changeStatus(id, "CONFIRMED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Check the UPI payment")));

        review(id, "APPROVE")
                .andExpect(jsonPath("$.order.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentNeedsReview").value(false));
        review(id, "APPROVE").andExpect(status().isConflict());
        changeStatus(id, "CONFIRMED").andExpect(status().isOk());
    }

    @Test
    void rejectedUpiPaymentsCannotBeConfirmed() throws Exception {
        String reference = JsonPath.read(mvc.perform(multipart("/api/me/payment-screenshots")
                        .file(new MockMultipartFile("file", "paid.png", "image/png", PNG))
                        .header("Authorization", customer))
                .andReturn().getResponse().getContentAsString(), "$.reference");
        String id = JsonPath.read(place("UPI", reference, 5, null), "$.id");

        review(id, "REJECT").andExpect(jsonPath("$.order.paymentStatus").value("FAILED"));
        changeStatus(id, "CONFIRMED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("The UPI payment was rejected")));
        changeStatus(id, "CANCELLED").andExpect(status().isOk());
    }

    @Test
    void findsCustomersToGiveCredit() throws Exception {
        String userId = JsonPath.read(mvc.perform(get("/api/me").header("Authorization", customer))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String phone = JsonPath.read(mvc.perform(get("/api/me").header("Authorization", customer))
                .andReturn().getResponse().getContentAsString(), "$.phone");

        mvc.perform(get("/api/admin/shops/customers").header("Authorization", admin).param("q", phone))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].fullName").value("Test Shop"))
                .andExpect(jsonPath("$[0].creditLimit").doesNotExist());
        mvc.perform(get("/api/admin/shops/customers").header("Authorization", admin).param("q", "a"))
                .andExpect(jsonPath("$", empty()));
        mvc.perform(get("/api/admin/shops/customers").header("Authorization", admin).param("q", "admin@smartretail"))
                .andExpect(jsonPath("$", empty()));

        json(put("/api/admin/shops/credit"), admin, "{\"userId\":\"" + userId + "\",\"creditLimit\":15000}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/shops/customers").header("Authorization", admin).param("q", phone))
                .andExpect(jsonPath("$[0].creditLimit").value(15000.0));
        mvc.perform(get("/api/admin/shops").header("Authorization", admin).param("q", phone))
                .andExpect(jsonPath("$.items[?(@.ownerUserId == '" + userId + "')].phone", contains(phone)));
    }

    // ---- helpers ---------------------------------------------------------

    private String placeCod(int cases, String coupon) throws Exception {
        return place("COD", null, cases, coupon);
    }

    /** 5 Sprite cases at ₹480 = ₹2,400 + ₹500 delivery. */
    private String place(String method, String screenshot, int cases, String coupon) throws Exception {
        return json(post("/api/orders"), customer, """
                {"items": [{"productId": "%s", "quantity": %d}], "couponCode": %s, "addressId": "%s",
                 "paymentMethod": "%s", "paymentScreenshot": %s}
                """.formatted(sprite, cases, coupon == null ? "null" : "\"" + coupon + "\"", address, method,
                screenshot == null ? "null" : "\"" + screenshot + "\""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private ResultActions changeStatus(String id, String status) throws Exception {
        return json(post("/api/admin/orders/" + id + "/status"), admin, "{\"status\":\"" + status + "\"}");
    }

    private ResultActions review(String id, String decision) throws Exception {
        return json(post("/api/admin/orders/" + id + "/payment-review"), admin, "{\"decision\":\"" + decision + "\"}");
    }

    private int stock() {
        return jdbc.queryForObject("select stock_quantity from products where slug = 'sprite-300ml-bottle'", Integer.class);
    }

    private ResultActions json(MockHttpServletRequestBuilder request,
            String bearer, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
