package com.chaoticteam.backend.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Client mistakes must answer 4xx with a JSON error, never a 500. */
class ErrorHandlingAcceptanceTest extends AcceptanceTest {

    @Test
    void unsupportedMethodIs405() throws Exception {
        Session user = signup();
        mvc.perform(as(put("/api/courses").contentType(JSON).content("{}"), user))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.error").isNotEmpty());
        mvc.perform(as(put("/api/profile").contentType(JSON).content("{}"), user))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unsupportedContentTypeIs415() throws Exception {
        Session user = signup();
        mvc.perform(as(post("/api/courses").contentType(MediaType.TEXT_PLAIN).content("name=Docker"), user))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void malformedJsonAndNonNumericIdsAreBadRequests() throws Exception {
        Session user = signup();
        mvc.perform(as(post("/api/courses").contentType(JSON).content("{not json"), user))
            .andExpect(status().isBadRequest());
        mvc.perform(as(get("/api/courses/abc"), user)).andExpect(status().isBadRequest());
    }

    @Test
    void anonymousRequestsStillGet401BeforeAnyOtherError() throws Exception {
        mvc.perform(put("/api/courses").contentType(JSON).content("{}")).andExpect(status().isUnauthorized());
    }
}
