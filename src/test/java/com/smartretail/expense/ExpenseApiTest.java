package com.smartretail.expense;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import com.smartretail.TestAuth;
import com.smartretail.common.BusinessCalendar;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ExpenseApiTest {

    /** A year of its own, so other tests' expenses do not change the totals. */
    private static final LocalDate DAY = LocalDate.of(2001, 3, 15);

    @Autowired
    MockMvc mvc;

    @Autowired
    BusinessCalendar calendar;

    String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = TestAuth.adminBearer(mvc);
    }

    private String expense(String title, String category, String amount, LocalDate date) {
        return """
                {"title": "%s", "category": "%s", "amount": %s, "expenseDate": "%s"}
                """.formatted(title, category, amount, date);
    }

    @Test
    void listsAndTotalsByDateAndCategory() throws Exception {
        LocalDate day = DAY.plusDays(100);
        json(post("/api/admin/expenses"), admin, expense("Diesel", "FUEL", "2000", day)).andExpect(status().isCreated());
        json(post("/api/admin/expenses"), admin, expense("More diesel", "FUEL", "500.50", day)).andExpect(status().isCreated());
        json(post("/api/admin/expenses"), admin, expense("Rent", "RENT", "15000", day)).andExpect(status().isCreated());
        json(post("/api/admin/expenses"), admin, expense("Outside range", "RENT", "1", day.plusDays(5))).andExpect(status().isCreated());

        mvc.perform(get("/api/admin/expenses/summary").header("Authorization", admin)
                        .param("from", day.toString()).param("to", day.toString()))
                .andExpect(jsonPath("$.total").value(17500.5))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.byCategory[*].category", contains("RENT", "FUEL")));
        mvc.perform(get("/api/admin/expenses").header("Authorization", admin)
                        .param("from", day.toString()).param("to", day.toString()).param("category", "RENT"))
                .andExpect(jsonPath("$.items[*].title", contains("Rent")));
        mvc.perform(get("/api/admin/expenses").header("Authorization", admin)
                        .param("from", day.toString()).param("to", day.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editsAndDeletes() throws Exception {
        String id = JsonPath.read(json(post("/api/admin/expenses"), admin, expense("Tyre", "MAINTENANCE", "100", DAY))
                .andReturn().getResponse().getContentAsString(), "$.id");
        json(put("/api/admin/expenses/" + id), admin, expense("New tyre", "TRANSPORT", "250", DAY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New tyre"))
                .andExpect(jsonPath("$.category").value("TRANSPORT"))
                .andExpect(jsonPath("$.amount").value(250));
        mvc.perform(delete("/api/admin/expenses/" + id).header("Authorization", admin)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/expenses/" + id).header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void checksInputAndAccess() throws Exception {
        json(post("/api/admin/expenses"), admin, expense("x", "FUEL", "0", DAY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("must be more than zero"));
        json(post("/api/admin/expenses"), admin, expense("x", "FUEL", "1.555", DAY)).andExpect(status().isBadRequest());
        json(post("/api/admin/expenses"), admin, expense("", "FUEL", "1", DAY)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
        json(post("/api/admin/expenses"), admin, expense("x", "NONSENSE", "1", DAY)).andExpect(status().isBadRequest());
        json(post("/api/admin/expenses"), admin, expense("x", "FUEL", "1", calendar.today().plusDays(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The expense date cannot be in the future"));
        mvc.perform(get("/api/admin/expenses").header("Authorization", TestAuth.customerBearer(mvc)))
                .andExpect(status().isForbidden());
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String bearer, String body) throws Exception {
        return mvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
