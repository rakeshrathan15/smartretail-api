package com.smartretail;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Gets access tokens for tests. The admin comes from application-dev.yml. */
public final class TestAuth {

    private static final AtomicInteger PHONE_SUFFIX = new AtomicInteger(50000);

    private TestAuth() {
    }

    public static String adminBearer(MockMvc mvc) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin@smartretail.local\",\"password\":\"Admin@12345\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    public static String customerBearer(MockMvc mvc) throws Exception {
        String phone = "97000" + PHONE_SUFFIX.incrementAndGet();
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Test Shop\",\"phone\":\"" + phone + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }
}
