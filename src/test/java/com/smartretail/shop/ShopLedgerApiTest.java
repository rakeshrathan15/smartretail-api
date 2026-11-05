package com.smartretail.shop;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ShopLedgerApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    String admin;
    String customer;
    String thumsUp;
    String address;
    String shop;

    /** A shop with a ₹20,000 limit and an address, ready to order on credit. */
    @BeforeEach
    void setUp() throws Exception {
        admin = TestAuth.adminBearer(mvc);
        customer = TestAuth.customerBearer(mvc);
        thumsUp = JsonPath.read(body(mvc.perform(get("/api/products/thums-up-750ml-pet"))), "$.data.id");
        // The catalog is shared with other tests, so these orders must not run it out of stock.
        jdbc.sql("UPDATE products SET stock_quantity = stock_quantity + 100 WHERE id = :id::uuid")
                .param("id", thumsUp).update();
        address = JsonPath.read(body(json(post("/api/me/addresses"), customer, """
                {"label": "Shop", "fullName": "Ravi", "phone": "9876501234", "addressLine1": "12 Main Road",
                 "city": "Hyderabad", "pincode": "500001"}
                """)), "$.data.id");
        String userId = JsonPath.read(body(mvc.perform(get("/api/me").header("Authorization", customer))), "$.data.id");
        shop = JsonPath.read(body(json(put("/api/admin/shops/credit"), admin,
                "{\"userId\":\"" + userId + "\",\"shopName\":\"Ledger Kirana\",\"creditLimit\":20000}")
                .andExpect(status().isOk())), "$.data.id");
    }

    @Test
    void repaymentsLowerWhatAShopOwesAndFreeItsCredit() throws Exception {
        double first = creditOrder();
        double second = creditOrder();
        double owed = first + second;
        ledger().andExpect(jsonPath("$.data.shop.outstanding").value(owed))
                .andExpect(jsonPath("$.data.openOrders", hasSize(2)));

        // Enough to clear the first order and part of the second.
        double paid = first + 100;
        pay(paid, "CHEQUE", "CHQ-1001").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shop.outstanding").value(owed - paid))
                .andExpect(jsonPath("$.data.paidTotal").value(paid))
                .andExpect(jsonPath("$.data.openOrders", hasSize(1)))
                .andExpect(jsonPath("$.data.openOrders[0].due").value(second - 100))
                .andExpect(jsonPath("$.data.payments[0].method").value("CHEQUE"))
                .andExpect(jsonPath("$.data.payments[0].reference").value("CHQ-1001"))
                .andExpect(jsonPath("$.data.payments[0].receivedBy").isNotEmpty())
                .andExpect(jsonPath("$.data.payments[0].voided").value(false));

        // Checkout sees the same balance.
        json(post("/api/checkout/quote"), customer, items(10))
                .andExpect(jsonPath("$.data.credit.outstanding").value(owed - paid))
                .andExpect(jsonPath("$.data.credit.available").value(20000 - owed + paid));
    }

    @Test
    void aVoidedPaymentStaysInTheHistoryButNotInTheBalance() throws Exception {
        double owed = creditOrder();
        String ledger = body(pay(1000, "CASH", null));
        String payment = JsonPath.read(ledger, "$.data.payments[0].id");

        json(post("/api/admin/shops/" + shop + "/payments/" + payment + "/void"), admin, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shop.outstanding").value(owed))
                .andExpect(jsonPath("$.data.paidTotal").value(0))
                .andExpect(jsonPath("$.data.payments", hasSize(1)))
                .andExpect(jsonPath("$.data.payments[0].voided").value(true))
                .andExpect(jsonPath("$.data.payments[0].voidedBy").isNotEmpty());

        json(post("/api/admin/shops/" + shop + "/payments/" + payment + "/void"), admin, "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value("This payment has already been voided"));
        // A payment can only be voided through its own shop.
        String other = JsonPath.read(body(json(post("/api/admin/shops"), admin, shopBody("Other Kirana", null, "0"))), "$.data.id");
        json(post("/api/admin/shops/" + other + "/payments/" + payment + "/void"), admin, "{}")
                .andExpect(status().isNotFound());
    }

    @Test
    void cancellingACreditOrderTakesItOffTheBalance() throws Exception {
        String order = JsonPath.read(body(placeCredit().andExpect(status().isCreated())), "$.data.id");
        json(post("/api/admin/orders/" + order + "/status"), admin, "{\"status\":\"CANCELLED\"}").andExpect(status().isOk());
        ledger().andExpect(jsonPath("$.data.shop.outstanding").value(0))
                .andExpect(jsonPath("$.data.openOrders", hasSize(0)));
    }

    @Test
    void payingMoreThanOwedIsAnAdvance() throws Exception {
        pay(1500, "UPI", "UTR1").andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shop.outstanding").value(-1500));
        json(post("/api/checkout/quote"), customer, items(10))
                .andExpect(jsonPath("$.data.credit.available").value(21500));
    }

    @Test
    void flagsShopsOverTheirLimit() throws Exception {
        creditOrder();
        double before = JsonPath.<Number>read(body(mvc.perform(get("/api/admin/shops/summary").header("Authorization", admin))), "$.data.overLimit").doubleValue();

        String userId = JsonPath.read(body(mvc.perform(get("/api/me").header("Authorization", customer))), "$.data.id");
        json(put("/api/admin/shops/credit"), admin, "{\"userId\":\"" + userId + "\",\"creditLimit\":100}").andExpect(status().isOk());

        ledger().andExpect(jsonPath("$.data.shop.overLimit").value(true));
        mvc.perform(get("/api/admin/shops/summary").header("Authorization", admin))
                .andExpect(jsonPath("$.data.overLimit").value(before + 1));
        mvc.perform(get("/api/admin/shops").header("Authorization", admin)
                        .param("standing", "OVER_LIMIT").param("size", "100"))
                .andExpect(jsonPath("$.data.items[*].id", hasItem(shop)));
        pay(100000, "BANK", null).andExpect(status().isCreated()).andExpect(jsonPath("$.data.shop.overLimit").value(false));
        mvc.perform(get("/api/admin/shops").header("Authorization", admin)
                        .param("standing", "OVER_LIMIT").param("size", "100"))
                .andExpect(jsonPath("$.data.items[*].id", not(hasItem(shop))));
    }

    @Test
    void aShopTurnedOffCannotOrderOnCreditButCanStillPay() throws Exception {
        creditOrder();
        json(patch("/api/admin/shops/" + shop + "/active"), admin, "{\"active\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        placeCredit().andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.message").value(startsWith("Credit is not enabled")));
        pay(500, "CASH", null).andExpect(status().isCreated());
    }

    @Test
    void adminsAddAndEditShopsByHand() throws Exception {
        String name = "Hand Kirana " + UUID.randomUUID();
        String route = JsonPath.read(body(json(post("/api/admin/routes"), admin,
                "{\"name\":\"Hand route " + UUID.randomUUID() + "\",\"days\":[]}").andExpect(status().isCreated())), "$.data.id");
        String id = JsonPath.read(body(json(post("/api/admin/shops"), admin, shopBody(name, route, "5000"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gstin").value("36ABCDE1234F1Z5"))
                .andExpect(jsonPath("$.data.phone").value("9876501234"))
                .andExpect(jsonPath("$.data.routeId").value(route))
                .andExpect(jsonPath("$.data.ownerUserId").isEmpty())), "$.data.id");

        json(put("/api/admin/shops/" + id), admin, shopBody(name, null, "7000")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeId").isEmpty())
                .andExpect(jsonPath("$.data.creditLimit").value(7000));
        mvc.perform(get("/api/admin/shops").header("Authorization", admin).param("q", name))
                .andExpect(jsonPath("$.data.items[*].id", contains(id)));

        // A shop with an account takes its route from its map pin.
        json(put("/api/admin/shops/" + shop), admin, shopBody("Ledger Kirana", route, "20000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("This shop's route comes from its map pin and cannot be changed here"));
        json(post("/api/admin/shops"), admin, shopBody(name, "00000000-0000-0000-0000-000000000001", "0"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checksInputAndAccess() throws Exception {
        json(post("/api/admin/shops/" + shop + "/payments"), admin, "{\"amount\":0,\"method\":\"CASH\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.amount").value("must be more than zero"));
        json(post("/api/admin/shops/" + shop + "/payments"), admin, "{\"amount\":10.555,\"method\":\"CASH\"}")
                .andExpect(status().isBadRequest());
        json(post("/api/admin/shops/" + shop + "/payments"), admin, "{\"amount\":10,\"method\":\"GOLD\"}")
                .andExpect(status().isBadRequest());
        json(post("/api/admin/shops/00000000-0000-0000-0000-000000000001/payments"), admin, "{\"amount\":10,\"method\":\"CASH\"}")
                .andExpect(status().isNotFound());
        json(post("/api/admin/shops"), admin, "{\"name\":\"\",\"gstin\":\"BAD\",\"pincode\":\"12\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.name").exists())
                .andExpect(jsonPath("$.error.details.gstin").exists())
                .andExpect(jsonPath("$.error.details.pincode").exists())
                .andExpect(jsonPath("$.error.details.creditLimit").exists());
        json(post("/api/admin/shops/" + shop + "/payments"), customer, "{\"amount\":10,\"method\":\"CASH\"}")
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/shops").header("Authorization", admin).param("standing", "NONSENSE"))
                .andExpect(status().isBadRequest());
    }

    private double creditOrder() throws Exception {
        return JsonPath.<Number>read(body(placeCredit().andExpect(status().isCreated())), "$.data.total").doubleValue();
    }

    private ResultActions placeCredit() throws Exception {
        return json(post("/api/orders"), customer, """
                {"items": [{"productId": "%s", "quantity": 10}], "addressId": "%s", "paymentMethod": "CREDIT"}
                """.formatted(thumsUp, address));
    }

    private String items(int cases) {
        return "{\"items\":[{\"productId\":\"" + thumsUp + "\",\"quantity\":" + cases + "}]}";
    }

    private ResultActions pay(double amount, String method, String reference) throws Exception {
        return json(post("/api/admin/shops/" + shop + "/payments"), admin, """
                {"amount": %s, "method": "%s", "reference": %s}
                """.formatted(amount, method, reference == null ? "null" : "\"" + reference + "\""));
    }

    private ResultActions ledger() throws Exception {
        return mvc.perform(get("/api/admin/shops/" + shop + "/ledger").header("Authorization", admin));
    }

    private static String shopBody(String name, String route, String limit) {
        return """
                {"name": "%s", "phone": "9876501234", "gstin": "36abcde1234f1z5", "address": "1 Main Road",
                 "city": "Hyderabad", "pincode": "500001", "deliveryRouteId": %s, "creditLimit": %s}
                """.formatted(name, route == null ? "null" : "\"" + route + "\"", limit);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String bearer, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
