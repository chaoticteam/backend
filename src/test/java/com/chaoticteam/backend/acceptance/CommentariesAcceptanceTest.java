package com.chaoticteam.backend.acceptance;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class CommentariesAcceptanceTest extends AcceptanceTest {

    private static String origin() {
        return "http://" + unique("site") + ".test";
    }

    @Test
    void commentsAreScopedToTheSiteInTheOriginHeader() throws Exception {
        Session user = signup();
        String siteA = origin();
        String siteB = origin();

        int id = idOf(mvc.perform(as(post("/api/commentaries").header(HttpHeaders.ORIGIN, siteA)
                .contentType(JSON).content("{\"comment\":\"hello A\"}"), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comment").value("hello A"))
            .andExpect(jsonPath("$.userId").isNumber())
            .andExpect(jsonPath("$.site.url").value(siteA))
            .andExpect(jsonPath("$.user").doesNotExist())
            .andReturn());
        mvc.perform(as(post("/api/commentaries").header(HttpHeaders.ORIGIN, siteB)
                .contentType(JSON).content("{\"comment\":\"hello B\"}"), user)).andExpect(status().isOk());

        // public listing, only that site's comments
        mvc.perform(get("/api/commentaries").header(HttpHeaders.ORIGIN, siteA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].comment").value("hello A"));
        mvc.perform(get("/api/commentaries").header(HttpHeaders.ORIGIN, origin()))
            .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/commentaries")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sameOriginReusesTheSite() throws Exception {
        Session user = signup();
        String site = origin();
        for (String text : new String[] {"one", "two"}) {
            mvc.perform(as(post("/api/commentaries").header(HttpHeaders.ORIGIN, site)
                    .contentType(JSON).content("{\"comment\":\"" + text + "\"}"), user)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/commentaries").header(HttpHeaders.ORIGIN, site)).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createNeedsAuthOriginAndComment() throws Exception {
        Session user = signup();
        mvc.perform(post("/api/commentaries").header(HttpHeaders.ORIGIN, origin())
                .contentType(JSON).content("{\"comment\":\"x\"}")).andExpect(status().isUnauthorized());
        mvc.perform(as(post("/api/commentaries").contentType(JSON).content("{\"comment\":\"x\"}"), user))
            .andExpect(status().isBadRequest());
        mvc.perform(as(post("/api/commentaries").header(HttpHeaders.ORIGIN, origin())
                .contentType(JSON).content("{\"comment\":\"  \"}"), user)).andExpect(status().isBadRequest());
    }

    @Test
    void detailNeedsAuthAndReturns404WhenMissing() throws Exception {
        Session user = signup();
        int id = idOf(mvc.perform(as(post("/api/commentaries").header(HttpHeaders.ORIGIN, origin())
                .contentType(JSON).content("{\"comment\":\"x\"}"), user)).andReturn());

        mvc.perform(get("/api/commentaries/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(as(get("/api/commentaries/" + id), user))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(as(get("/api/commentaries/987654321"), user))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Commentary not found"));
    }

    @Test
    void onlyTheAuthorCanDelete() throws Exception {
        Session author = signup();
        Session other = signup();
        String site = origin();
        int id = idOf(mvc.perform(as(post("/api/commentaries").header(HttpHeaders.ORIGIN, site)
                .contentType(JSON).content("{\"comment\":\"mine\"}"), author)).andReturn());

        mvc.perform(delete("/api/commentaries/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(as(delete("/api/commentaries/" + id), other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/commentaries").header(HttpHeaders.ORIGIN, site)).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(as(delete("/api/commentaries/" + id), author)).andExpect(status().isNoContent());
        mvc.perform(get("/api/commentaries").header(HttpHeaders.ORIGIN, site)).andExpect(jsonPath("$", hasSize(0)));
    }
}
