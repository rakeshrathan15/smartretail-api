package com.smartretail.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.smartretail.IntegrationTest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class AuthFlowTest {

    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(1000);

    @Autowired
    MockMvc mvc;

    @Test
    void registerThenLoginByEmailOrPhoneAndReadProfile() throws Exception {
        String phone = uniquePhone();
        String email = "Owner" + phone + "@Example.com";
        register("Ravi Kumar", email, "+91 " + phone, "secret123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.user.phone").value(phone))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.shopProfileComplete").value(false));

        String byPhone = login("0" + phone, "secret123").andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        login(email.toUpperCase(), "secret123").andExpect(status().isOk());

        mvc.perform(get("/api/me").header("Authorization", bearer(JsonPath.read(byPhone, "$.accessToken"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ravi Kumar"))
                .andExpect(jsonPath("$.phone").value(phone));
    }

    @Test
    void rejectsDuplicatesBadInputAndWrongPassword() throws Exception {
        String phone = uniquePhone();
        register("A", null, phone, "secret123").andExpect(status().isCreated());
        register("B", null, phone, "secret123").andExpect(status().isConflict());
        register("C", null, null, "secret123").andExpect(status().isBadRequest());
        register("D", null, "12345", "secret123").andExpect(status().isBadRequest());
        register("E", "not-an-email", null, "secret123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        register("F", null, uniquePhone(), "short").andExpect(status().isBadRequest());

        login(phone, "wrong-password").andExpect(status().isUnauthorized());
        login("nobody@example.com", "secret123").andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokensAndDetectsReuse() throws Exception {
        String first = register("Rotator", null, uniquePhone(), "secret123").andReturn()
                .getResponse().getContentAsString();
        String refresh1 = JsonPath.read(first, "$.refreshToken");

        String second = refresh(refresh1).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String refresh2 = JsonPath.read(second, "$.refreshToken");
        assertThat(refresh2).isNotEqualTo(refresh1);

        // Replaying the used token is treated as theft: it fails and kills the newer token too.
        refresh(refresh1).andExpect(status().isUnauthorized());
        refresh(refresh2).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String body = register("Leaver", null, uniquePhone(), "secret123").andReturn()
                .getResponse().getContentAsString();
        String refreshToken = JsonPath.read(body, "$.refreshToken");

        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());
        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void enforcesRolesOnProtectedRoutes() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me").header("Authorization", "Bearer not.a.jwt")).andExpect(status().isUnauthorized());

        String customer = JsonPath.read(register("Cust", null, uniquePhone(), "secret123").andReturn()
                .getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/admin/ping").header("Authorization", bearer(customer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/pos/ping").header("Authorization", bearer(customer))).andExpect(status().isForbidden());

        // The dev profile seeds an admin from application-dev.yml.
        String admin = JsonPath.read(login("admin@smartretail.local", "Admin@12345").andExpect(status().isOk())
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        // No admin endpoints exist yet, so getting past security means a 404 rather than 403.
        mvc.perform(get("/api/admin/ping").header("Authorization", bearer(admin))).andExpect(status().isNotFound());
    }

    private ResultActions register(String fullName, String email, String phone, String password) throws Exception {
        String json = "{\"fullName\":%s,\"email\":%s,\"phone\":%s,\"password\":%s}"
                .formatted(quote(fullName), quote(email), quote(phone), quote(password));
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions login(String identifier, String password) throws Exception {
        String json = "{\"identifier\":%s,\"password\":%s}".formatted(quote(identifier), quote(password));
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private static String uniquePhone() {
        return "98765" + String.format("%05d", PHONE_SUFFIX.incrementAndGet());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
