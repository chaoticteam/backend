package com.chaoticteam.backend.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/**
 * Acceptance tests: the whole app (security, controllers, JPA) against a real
 * PostgreSQL, driven through HTTP with MockMvc. Every test creates its own users
 * so tests are independent of each other and of execution order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AcceptanceTest {

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired
    protected MockMvc mvc;

    /** A signed-up user and its JWT. */
    protected record Session(String username, String token) {
    }

    protected static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String signupBody(String username) {
        return """
            {"userName":"%s","password":"password","email":"%s@example.io",
             "profile":{"firstName":"alex","lastName":"diaz"}}""".formatted(username, username);
    }

    protected Session signup() throws Exception {
        String username = unique("user");
        String body = mvc.perform(post("/api/auth/signup").contentType(JSON).content(signupBody(username)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return new Session(username, JsonPath.read(body, "$.token"));
    }

    protected static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, Session session) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token());
    }

    protected static int idOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
