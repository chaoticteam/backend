package com.chaoticteam.backend.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The published OpenAPI (Swagger) must document every endpoint of
 * https://al3xdiaz.github.io/go-server/ (paths under /api).
 */
class ApiDocumentationContractTest extends AcceptanceTest {

    /** Every request of the go-server documentation. */
    private static final List<String> GO_SERVER_DOC = List.of(
        "POST /auth/signup", "GET /auth/userdata", "POST /auth/login", "POST /auth/validatecredetial",
        "GET /commentaries", "POST /commentaries", "GET /commentaries/{id}", "DELETE /commentaries/{id}",
        "GET /courses", "POST /courses", "PATCH /courses/{id}", "DELETE /courses/{id}",
        "GET /achievements", "POST /achievements", "PATCH /achievements/{id}", "DELETE /achievements/{id}",
        "GET /vcard/{username}", "PATCH /profile", "GET /profile", "POST /telephone", "DELETE /telephone/{id}",
        "GET /users", "GET /projects", "POST /projects", "PATCH /projects/{id}", "DELETE /projects/{id}",
        "GET /galleries", "POST /galleries", "DELETE /galleries/{id}", "GET /version");

    @Autowired
    private ObjectMapper mapper;

    private JsonNode api;

    private JsonNode api() throws Exception {
        if (api == null) {
            String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
            api = mapper.readTree(body);
        }
        return api;
    }

    @Test
    void documentsEveryEndpointOfTheGoServerDocumentation() throws Exception {
        Set<String> documented = new HashSet<>();
        api().get("paths").fields().forEachRemaining(path ->
            path.getValue().fieldNames().forEachRemaining(method ->
                documented.add(method.toUpperCase() + " " + path.getKey().replaceFirst("^/api", ""))));

        assertThat(documented).containsAll(GO_SERVER_DOC);
    }

    @Test
    void groupsOperationsInOneTagPerModule() throws Exception {
        Set<String> tags = new HashSet<>();
        api().get("paths").forEach(path -> path.forEach(operation ->
            operation.get("tags").forEach(tag -> tags.add(tag.asText()))));

        assertThat(tags).contains("Auth", "Commentaries", "Courses", "Achievements", "Projects", "Galleries", "Profile", "Config");
    }

    @Test
    void publicEndpointsDeclareNoSecurityAndProtectedOnesInheritBearer() throws Exception {
        JsonNode paths = api().get("paths");
        assertThat(paths.at("/~1api~1courses/get/security")).hasSize(0);
        assertThat(paths.at("/~1api~1auth~1login/post/security")).hasSize(0);
        assertThat(paths.at("/~1api~1courses/post/security").isMissingNode()).isTrue();
        assertThat(api().at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
    }

    @Test
    void requestBodiesAreNotDocumentedAsQueryParameters() throws Exception {
        JsonNode paths = api().get("paths");
        assertThat(paths.at("/~1api~1auth~1login/post/parameters").isMissingNode()).isTrue();
        assertThat(paths.at("/~1api~1auth~1signup/post/parameters").isMissingNode()).isTrue();
        assertThat(paths.at("/~1api~1courses/post/requestBody/content/application~1json/examples/create").isMissingNode()).isFalse();
    }
}
