package com.smartretail.inventory;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import com.smartretail.common.BusinessCalendar;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class InventoryApiTest {

    private static final String NO_PRODUCT = "00000000-0000-0000-0000-000000000001";

    @Autowired
    MockMvc mvc;

    @Autowired
    BusinessCalendar calendar;

    String admin;
    String product;
    String otherProduct;
    String supplier;

    @BeforeEach
    void setUp() throws Exception {
        admin = TestAuth.adminBearer(mvc);
        product = newProduct(10);
        otherProduct = newProduct(0);
        supplier = JsonPath.read(body(json(post("/api/admin/suppliers"), admin,
                "{\"name\":\"Coca-Cola Distributor\",\"phone\":\"0870 2345678\",\"gstin\":\"36abcde1234f1z5\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gstin").value("36ABCDE1234F1Z5"))), "$.id");
    }

    @Test
    void aReceiptRaisesStockAndUpdatesCost() throws Exception {
        String invoice = "INV-" + UUID.randomUUID();
        receive(supplier, invoice, calendar.today().toString(), item(product, 5, "100.50") + "," + item(otherProduct, 2, "40"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receipt.totalCost").value(582.5))
                .andExpect(jsonPath("$.receipt.supplierName").value("Coca-Cola Distributor"))
                .andExpect(jsonPath("$.receipt.itemCount").value(2))
                .andExpect(jsonPath("$.receipt.totalCases").value(7))
                .andExpect(jsonPath("$.receipt.createdBy").isNotEmpty())
                .andExpect(jsonPath("$.items[?(@.productId == '" + product + "')].total").value(hasItem(502.5)));

        stockRow(product).andExpect(jsonPath("$.items[0].stock").value(15))
                .andExpect(jsonPath("$.items[0].costPrice").value(100.5));
        stockRow(otherProduct).andExpect(jsonPath("$.items[0].stock").value(2));
        mvc.perform(get("/api/admin/inventory/receipts").header("Authorization", admin).param("supplierId", supplier))
                .andExpect(jsonPath("$.items[*].invoiceNumber", contains(invoice)));
    }

    @Test
    void theSameBillCannotBeEnteredTwice() throws Exception {
        receive(supplier, "BILL-7", calendar.today().toString(), item(product, 5, "100")).andExpect(status().isCreated());
        receive(supplier, "bill-7", calendar.today().toString(), item(product, 5, "100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Bill bill-7 from Coca-Cola Distributor has already been entered"));
        stockRow(product).andExpect(jsonPath("$.items[0].stock").value(15));
    }

    @Test
    void aBadReceiptChangesNothing() throws Exception {
        String today = calendar.today().toString();
        receive(supplier, null, today, item(product, 5, "100") + "," + item(NO_PRODUCT, 1, "1")).andExpect(status().isNotFound());
        receive(supplier, null, today, item(product, 1, "1") + "," + item(product, 1, "1")).andExpect(status().isBadRequest());
        receive(supplier, null, calendar.today().plusDays(1).toString(), item(product, 1, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The receipt date cannot be in the future"));
        receive(supplier, null, today, item(product, 0, "1")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
        receive(supplier, null, today, "").andExpect(status().isBadRequest());
        json(patch("/api/admin/suppliers/" + supplier + "/active"), admin, "{\"active\":false}").andExpect(status().isOk());
        receive(supplier, null, today, item(product, 1, "1")).andExpect(status().isBadRequest());
        stockRow(product).andExpect(jsonPath("$.items[0].stock").value(10));
    }

    @Test
    void adjustmentsChangeStockAndKeepAHistory() throws Exception {
        adjust(product, -3, "DAMAGE", "Dropped").andExpect(status().isCreated());
        adjust(product, 1, "CORRECTION", null).andExpect(status().isCreated());
        stockRow(product).andExpect(jsonPath("$.items[0].stock").value(8));
        mvc.perform(get("/api/admin/inventory/adjustments").header("Authorization", admin).param("productId", product))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[*].delta", contains(1, -3)))
                .andExpect(jsonPath("$.items[1].reason").value("DAMAGE"))
                .andExpect(jsonPath("$.items[1].notes").value("Dropped"))
                .andExpect(jsonPath("$.items[1].createdBy").isNotEmpty());

        adjust(product, -9, "DAMAGE", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.startsWith("Only 8 cases of")));
        adjust(product, 0, "CORRECTION", null).andExpect(status().isBadRequest());
        adjust(product, 1, "OTHER", " ").andExpect(status().isBadRequest());
        adjust(NO_PRODUCT, 1, "CORRECTION", null).andExpect(status().isNotFound());
        stockRow(product).andExpect(jsonPath("$.items[0].stock").value(8));
    }

    @Test
    void showsLowAndOutOfStockAndTotals() throws Exception {
        String low = newProduct(3);
        mvc.perform(get("/api/admin/inventory").header("Authorization", admin).param("level", "LOW").param("size", "100"))
                .andExpect(jsonPath("$.items[*].productId", hasItem(low)))
                .andExpect(jsonPath("$.items[*].productId", not(hasItem(product))));
        mvc.perform(get("/api/admin/inventory").header("Authorization", admin).param("level", "OUT").param("size", "100"))
                .andExpect(jsonPath("$.items[*].productId", hasItem(otherProduct)));
        stockRow(low).andExpect(jsonPath("$.items[0].level").value("LOW"))
                .andExpect(jsonPath("$.items[0].stockValue").value(1500));

        double before = number("$.stockValue");
        double profitBefore = number("$.potentialProfit");
        adjust(product, 2, "CORRECTION", null).andExpect(status().isCreated());
        org.assertj.core.api.Assertions.assertThat(number("$.stockValue")).isEqualTo(before + 1000);
        org.assertj.core.api.Assertions.assertThat(number("$.potentialProfit")).isEqualTo(profitBefore + 200);
    }

    @Test
    void editingAProductCannotChangeStockBehindTheHistory() throws Exception {
        json(put("/api/admin/products/" + product), admin, productBody("Renamed " + UUID.randomUUID(), 999))
                .andExpect(status().isOk());
        stockRow(product).andExpect(jsonPath("$.items[0].stock").value(10));
    }

    @Test
    void aProductWithReceiptsCannotBeDeleted() throws Exception {
        receive(supplier, null, calendar.today().toString(), item(product, 1, "1")).andExpect(status().isCreated());
        mvc.perform(delete("/api/admin/products/" + product).header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void suppliersAreEditableAndAdminOnly() throws Exception {
        json(put("/api/admin/suppliers/" + supplier), admin, "{\"name\":\"Renamed\",\"notes\":\"Calls on Monday\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.phone").isEmpty());
        json(post("/api/admin/suppliers"), admin, "{\"name\":\"\",\"phone\":\"x\",\"gstin\":\"BAD\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.gstin").exists());
        String customer = TestAuth.customerBearer(mvc);
        mvc.perform(get("/api/admin/suppliers").header("Authorization", customer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/inventory").header("Authorization", customer)).andExpect(status().isForbidden());
        adjustAs(customer).andExpect(status().isForbidden());
    }

    private ResultActions adjustAs(String bearer) throws Exception {
        return json(post("/api/admin/inventory/adjustments"), bearer,
                "{\"productId\":\"" + product + "\",\"delta\":1,\"reason\":\"CORRECTION\"}");
    }

    private double number(String path) throws Exception {
        return JsonPath.<Number>read(body(mvc.perform(get("/api/admin/inventory/summary").header("Authorization", admin))), path)
                .doubleValue();
    }

    private String newProduct(int stock) throws Exception {
        return JsonPath.read(body(json(post("/api/admin/products"), admin, productBody("Stock Test " + UUID.randomUUID(), stock))
                .andExpect(status().isCreated())), "$.product.id");
    }

    private static String productBody(String name, int stock) {
        return """
                {"name": "%s", "unitsPerCase": 24, "price": 600, "costPrice": 500, "stockQuantity": %d, "lowStockThreshold": 5}
                """.formatted(name, stock);
    }

    private ResultActions stockRow(String productId) throws Exception {
        String name = JsonPath.read(body(mvc.perform(get("/api/admin/products/" + productId).header("Authorization", admin))), "$.product.name");
        return mvc.perform(get("/api/admin/inventory").header("Authorization", admin).param("q", name));
    }

    private static String item(String productId, int quantity, String cost) {
        return "{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + ",\"costPrice\":" + cost + "}";
    }

    private ResultActions receive(String supplierId, String invoice, String date, String items) throws Exception {
        return json(post("/api/admin/inventory/receipts"), admin, """
                {"supplierId": "%s", "receiptDate": "%s", "invoiceNumber": %s, "items": [%s]}
                """.formatted(supplierId, date, invoice == null ? "null" : "\"" + invoice + "\"", items));
    }

    private ResultActions adjust(String productId, int delta, String reason, String notes) throws Exception {
        return json(post("/api/admin/inventory/adjustments"), admin, """
                {"productId": "%s", "delta": %d, "reason": "%s", "notes": %s}
                """.formatted(productId, delta, reason, notes == null ? "null" : "\"" + notes + "\""));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String bearer, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
