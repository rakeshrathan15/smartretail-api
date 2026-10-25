package com.smartretail.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Checkout against the dev seed: delivery ₹500, free above ₹5,000, minimum order ₹1,000. */
@IntegrationTest
class CheckoutApiTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    String customer;
    String thumsUp;
    String bisleri;
    String monster;

    @BeforeEach
    void setUp() throws Exception {
        customer = TestAuth.customerBearer(mvc);
        thumsUp = productId("thums-up-750ml-pet");
        bisleri = productId("bisleri-1l-packaged-water");
        monster = productId("monster-sugar-free-350ml");
    }

    @Test
    void quotesSlabPricesCouponAndDelivery() throws Exception {
        // 10 × ₹378 (10+ slab) + 2 × ₹240 = ₹4,260; SAVE5 = ₹213; ₹4,047 is below ₹5,000 so delivery ₹500.
        quote(items(thumsUp, 10, bisleri, 2), "save5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(4260.0))
                .andExpect(jsonPath("$.lines[0].pricePerCase").value(378.0))
                .andExpect(jsonPath("$.lines[0].saving").value(420.0))
                .andExpect(jsonPath("$.couponCode").value("SAVE5"))
                .andExpect(jsonPath("$.couponDiscount").value(213.0))
                .andExpect(jsonPath("$.deliveryCharge").value(500.0))
                .andExpect(jsonPath("$.amountForFreeDelivery").value(953.0))
                .andExpect(jsonPath("$.total").value(4547.0))
                .andExpect(jsonPath("$.totalCases").value(12))
                .andExpect(jsonPath("$.upiId").value("smartretail@upi"))
                .andExpect(jsonPath("$.credit").doesNotExist())
                .andExpect(jsonPath("$.canPlaceOrder").value(true));
    }

    @Test
    void blocksSmallOrdersAndTooManyCases() throws Exception {
        quote(items(bisleri, 2), null)
                .andExpect(jsonPath("$.canPlaceOrder").value(false))
                .andExpect(jsonPath("$.problems[0]").value("Minimum order is ₹1,000. Add ₹520 more."));
        quote(items(monster, 500), null)
                .andExpect(jsonPath("$.lines[0].problem").value(startsWith("Only ")))
                .andExpect(jsonPath("$.canPlaceOrder").value(false));
        quote(items(bisleri, 5), "NOSUCHCODE")
                .andExpect(jsonPath("$.couponProblem").value("This coupon code is not valid"))
                .andExpect(jsonPath("$.couponDiscount").value(0.0));
        mvc.perform(post("/api/checkout/quote").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placesCashOnDeliveryOrderAndTakesStock() throws Exception {
        String address = createAddress(customer, "Shop");
        int stockBefore = stock(thumsUp);
        int couponUsesBefore = jdbc.queryForObject("select used_count from coupons where code = 'FLAT100'", Integer.class);

        String body = place(customer, items(thumsUp, 10, bisleri, 2), "flat100", address, "COD", null, 4660)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value(startsWith("SR")))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentTerms").value("PREPAID"))
                .andExpect(jsonPath("$.subtotal").value(4260.0))
                .andExpect(jsonPath("$.discount").value(100.0))
                .andExpect(jsonPath("$.deliveryCharge").value(500.0))
                .andExpect(jsonPath("$.total").value(4660.0))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].productName").value("Thums Up 750ml PET"))
                .andExpect(jsonPath("$.address.city").value("Hyderabad"))
                .andExpect(jsonPath("$.address.phone").value("9876501234"))
                .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(body, "$.id");

        assertThat(stock(thumsUp)).isEqualTo(stockBefore - 10);
        assertThat(jdbc.queryForObject("select used_count from coupons where code = 'FLAT100'", Integer.class))
                .isEqualTo(couponUsesBefore + 1);

        mvc.perform(get("/api/me/orders").header("Authorization", customer))
                .andExpect(jsonPath("$.items[0].id").value(orderId))
                .andExpect(jsonPath("$.items[0].totalCases").value(12));
        mvc.perform(get("/api/me/orders/" + orderId).header("Authorization", customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").value("FLAT100"));

        // Another shop cannot see it.
        mvc.perform(get("/api/me/orders/" + orderId).header("Authorization", TestAuth.customerBearer(mvc)))
                .andExpect(status().isNotFound());
    }

    @Test
    void refusesOrdersThatChangedOrCannotBeFilled() throws Exception {
        String address = createAddress(customer, "Shop");
        int stockBefore = stock(thumsUp);

        place(customer, items(thumsUp, 10), null, address, "COD", null, 3000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Prices have changed")));
        place(customer, items(monster, 500), null, address, "COD", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Only ")));
        place(customer, items(bisleri, 2), null, address, "COD", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Minimum order")));
        place(customer, items(thumsUp, 10), "NOSUCHCODE", address, "COD", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This coupon code is not valid"));
        place(customer, items(thumsUp, 10), null, address, "CASH", null, null)
                .andExpect(status().isBadRequest());

        // Someone else's address cannot be used.
        String otherAddress = createAddress(TestAuth.customerBearer(mvc), "Other");
        place(customer, items(thumsUp, 10), null, otherAddress, "COD", null, null).andExpect(status().isNotFound());

        // Nothing was taken from stock by the refused orders.
        assertThat(stock(thumsUp)).isEqualTo(stockBefore);
    }

    @Test
    void twoShopsCannotBuyTheSameLastCases() throws Exception {
        String redBull = productId("red-bull-energy-250ml");
        jdbc.update("update products set stock_quantity = 20 where id = ?::uuid", redBull);

        String[] shops = {TestAuth.customerBearer(mvc), TestAuth.customerBearer(mvc)};
        String[] addresses = {createAddress(shops[0], "Shop"), createAddress(shops[1], "Shop")};
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int shop = i;
                results.add(pool.submit(() -> {
                    start.await();
                    return place(shops[shop], items(redBull, 15), null, addresses[shop], "COD", null, null)
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(stock(redBull)).isEqualTo(5);
    }

    @Test
    void upiOrdersNeedAPaymentScreenshot() throws Exception {
        String address = createAddress(customer, "Shop");
        place(customer, items(thumsUp, 10), null, address, "UPI", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Upload the screenshot of your UPI payment"));

        String upload = mvc.perform(multipart("/api/me/payment-screenshots")
                        .file(new MockMultipartFile("file", "paid.png", "image/png", PNG))
                        .header("Authorization", customer))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(startsWith("payments/")))
                .andReturn().getResponse().getContentAsString();

        place(customer, items(thumsUp, 10), null, address, "UPI", JsonPath.read(upload, "$.reference"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING_VERIFICATION"));
    }

    @Test
    void creditOrdersStayWithinTheShopsLimit() throws Exception {
        String address = createAddress(customer, "Shop");
        place(customer, items(thumsUp, 10), null, address, "CREDIT", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Credit is not enabled")));

        String userId = JsonPath.read(mvc.perform(get("/api/me").header("Authorization", customer))
                .andReturn().getResponse().getContentAsString(), "$.id");
        String admin = TestAuth.adminBearer(mvc);
        mvc.perform(put("/api/admin/shops/credit").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"shopName\":\"Ravi Kirana\",\"creditLimit\":5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(5000))
                .andExpect(jsonPath("$.outstanding").value(0));

        quote(items(thumsUp, 10), null).andExpect(jsonPath("$.credit.available").value(5000.0));

        // ₹3,780 + ₹500 delivery = ₹4,280 on credit, leaving ₹720.
        place(customer, items(thumsUp, 10), null, address, "CREDIT", null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentTerms").value("CREDIT"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT"));
        quote(items(thumsUp, 10), null)
                .andExpect(jsonPath("$.credit.outstanding").value(4280.0))
                .andExpect(jsonPath("$.credit.available").value(720.0));
        place(customer, items(thumsUp, 10), null, address, "CREDIT", null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Not enough credit: ₹720 available")));
    }

    @Test
    void refusesOrdersWhileTheStoreIsClosed() throws Exception {
        String address = createAddress(customer, "Shop");
        jdbc.update("update store_settings set value = 'false' where key = 'store_open'");
        try {
            quote(items(thumsUp, 10), null)
                    .andExpect(jsonPath("$.storeOpen").value(false))
                    .andExpect(jsonPath("$.problems[0]").value(startsWith("The store is closed")));
            place(customer, items(thumsUp, 10), null, address, "COD", null, null)
                    .andExpect(status().isConflict());
        } finally {
            jdbc.update("update store_settings set value = 'true' where key = 'store_open'");
        }
    }

    @Test
    void managesAddressesAndTheDefault() throws Exception {
        String first = createAddress(customer, "Shop");
        String second = JsonPath.read(perform(post("/api/me/addresses"), customer, """
                {"label": "Godown", "fullName": "Ravi", "phone": "+91 98765 01234", "addressLine1": "Plot 9",
                 "city": "Secunderabad", "pincode": "500003", "makeDefault": true}
                """).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/me/addresses").header("Authorization", customer))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[0].state").value("Telangana"))
                .andExpect(jsonPath("$[1].isDefault").value(false));

        mvc.perform(delete("/api/me/addresses/" + second).header("Authorization", customer))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/me/addresses").header("Authorization", customer))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(first))
                .andExpect(jsonPath("$[0].isDefault").value(true));

        perform(post("/api/me/addresses"), customer, """
                {"fullName": "Ravi", "phone": "9876501234", "addressLine1": "x", "city": "y", "pincode": "5000"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.pincode").value("must be a 6-digit PIN code"));
        mvc.perform(delete("/api/me/addresses/" + first).header("Authorization", TestAuth.customerBearer(mvc)))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminsManageCoupons() throws Exception {
        String admin = TestAuth.adminBearer(mvc);
        perform(post("/api/admin/coupons"), admin, """
                {"code": "diwali10", "discountType": "PERCENTAGE", "discountValue": 10, "maxDiscount": 1000}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DIWALI10"));
        perform(post("/api/admin/coupons"), admin, """
                {"code": "DIWALI10", "discountType": "FIXED", "discountValue": 50}
                """).andExpect(status().isConflict());
        perform(post("/api/admin/coupons"), admin, """
                {"code": "HALFPLUS", "discountType": "PERCENTAGE", "discountValue": 150}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("above 100")));
        perform(post("/api/admin/coupons"), customer, """
                {"code": "MINE", "discountType": "FIXED", "discountValue": 5000}
                """).andExpect(status().isForbidden());

        quote(items(thumsUp, 10), "DIWALI10").andExpect(jsonPath("$.couponDiscount").value(378.0));
    }

    // ---- helpers ---------------------------------------------------------

    private String productId(String slug) throws Exception {
        return JsonPath.read(mvc.perform(get("/api/products/" + slug)).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private int stock(String productId) throws Exception {
        return JsonPath.read(mvc.perform(get("/api/admin/products/" + productId).header("Authorization", TestAuth.adminBearer(mvc)))
                .andReturn().getResponse().getContentAsString(), "$.stockQuantity");
    }

    private String createAddress(String bearer, String label) throws Exception {
        return JsonPath.read(perform(post("/api/me/addresses"), bearer, """
                {"label": "%s", "fullName": "Ravi Kumar", "phone": "9876501234", "addressLine1": "12 Main Road",
                 "city": "Hyderabad", "pincode": "500001"}
                """.formatted(label)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static String items(Object... idsAndQuantities) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < idsAndQuantities.length; i += 2) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"productId\":\"").append(idsAndQuantities[i]).append("\",\"quantity\":")
                    .append(idsAndQuantities[i + 1]).append('}');
        }
        return json.append(']').toString();
    }

    private ResultActions quote(String items, String coupon) throws Exception {
        return perform(post("/api/checkout/quote"), customer,
                "{\"items\":" + items + ",\"couponCode\":" + (coupon == null ? "null" : "\"" + coupon + "\"") + "}");
    }

    private ResultActions place(String bearer, String items, String coupon, String addressId, String method,
            String screenshot, Integer expectedTotal) throws Exception {
        return perform(post("/api/orders"), bearer, """
                {"items": %s, "couponCode": %s, "addressId": "%s", "paymentMethod": "%s",
                 "paymentScreenshot": %s, "expectedTotal": %s}
                """.formatted(items, coupon == null ? "null" : "\"" + coupon + "\"", addressId, method,
                screenshot == null ? "null" : "\"" + screenshot + "\"", expectedTotal));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer, String json) throws Exception {
        return mvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
