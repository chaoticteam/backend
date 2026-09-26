package com.chaoticteam.backend.acceptance;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

class AuthAcceptanceTest extends AcceptanceTest {

    @Test
    void signupAcceptsUserNameAndReturnsUserTokenAndCookie() throws Exception {
        String username = unique("signup");
        mvc.perform(post("/api/auth/signup").contentType(JSON).content(signupBody(username)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.userName").value(username))
            .andExpect(jsonPath("$.user.email").value(username + "@example.io"))
            .andExpect(jsonPath("$.user.verified").value(false))
            .andExpect(jsonPath("$.user.profile.firstName").value("alex"))
            .andExpect(jsonPath("$.user.profile.lastName").value("diaz"))
            .andExpect(jsonPath("$.user.profile.telephone").isArray())
            .andExpect(jsonPath("$.user.password").doesNotExist())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));
    }

    @Test
    void signupAlsoAcceptsLowerCaseUsername() throws Exception {
        String username = unique("lower");
        mvc.perform(post("/api/auth/signup").contentType(JSON).content(
                signupBody(username).replace("\"userName\"", "\"username\"")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.userName").value(username));
    }

    @Test
    void signupRejectsDuplicatedUserAndEmail() throws Exception {
        Session existing = signup();
        mvc.perform(post("/api/auth/signup").contentType(JSON).content(signupBody(existing.username())))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/signup").contentType(JSON).content(
                signupBody(unique("other")).replaceAll("\"email\":\"[^\"]+\"", "\"email\":\"" + existing.username() + "@example.io\"")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsUserTokenAndCookie() throws Exception {
        Session user = signup();
        mvc.perform(post("/api/auth/login").contentType(JSON)
                .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(user.username())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.userName").value(user.username()))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=")));
    }

    @Test
    void loginWithWrongPasswordOrUnknownUserIs401() throws Exception {
        Session user = signup();
        mvc.perform(post("/api/auth/login").contentType(JSON)
                .content("{\"username\":\"%s\",\"password\":\"nope\"}".formatted(user.username())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("invalid credentials"));
        mvc.perform(post("/api/auth/login").contentType(JSON)
                .content("{\"username\":\"ghost-user\",\"password\":\"password\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void userdataNeedsAuthAndAcceptsBearerOrCookie() throws Exception {
        Session user = signup();
        mvc.perform(get("/api/auth/userdata")).andExpect(status().isUnauthorized());

        mvc.perform(as(get("/api/auth/userdata"), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value(user.username()))
            .andExpect(jsonPath("$.profile.firstName").value("alex"));

        mvc.perform(get("/api/auth/userdata").cookie(new Cookie("access_token", user.token())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value(user.username()));
    }

    @Test
    void invalidTokenIsRejectedOnProtectedEndpoints() throws Exception {
        mvc.perform(get("/api/auth/userdata").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/userdata").cookie(new Cookie("access_token", "garbage")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpointsIgnoreAStaleCookie() throws Exception {
        Cookie stale = new Cookie("access_token", "garbage");
        mvc.perform(get("/api/courses").param("username", "nobody").cookie(stale))
            .andExpect(status().isOk());
        mvc.perform(get("/api/version").cookie(stale)).andExpect(status().isOk());
        Session user = signup();
        mvc.perform(post("/api/auth/login").contentType(JSON).cookie(stale)
                .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(user.username())))
            .andExpect(status().isOk());
    }

    @Test
    void validateCredentialStoresTheBearerTokenInTheCookie() throws Exception {
        Session user = signup();
        mvc.perform(as(post("/api/auth/validatecredetial"), user))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("access_token=" + user.token())));
        mvc.perform(post("/api/auth/validatecredetial")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutClearsTheCookie() throws Exception {
        Session user = signup();
        mvc.perform(as(delete("/api/auth/logout"), user))
            .andExpect(status().isNoContent())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
        mvc.perform(delete("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshIssuesNewTokensAndRejectsInvalidOnes() throws Exception {
        Session user = signup();
        String login = mvc.perform(post("/api/auth/login").contentType(JSON)
                .content("{\"username\":\"%s\",\"password\":\"password\"}".formatted(user.username())))
            .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(login, "$.refreshToken");

        mvc.perform(post("/api/auth/refresh").contentType(JSON).content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.userName").value(user.username()))
            .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(post("/api/auth/refresh").contentType(JSON).content("{\"refreshToken\":\"bad\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void versionAndHealthArePublic() throws Exception {
        mvc.perform(get("/api/version")).andExpect(status().isOk());
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
