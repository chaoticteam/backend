package com.chaoticteam.backend.acceptance;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Behavior shared by courses, achievements, projects and galleries (same rules as go-server):
 * public listing by username, owner-only writes, 404 for other people's data, 204 on delete.
 */
class OwnedResourcesAcceptanceTest extends AcceptanceTest {

    record Resource(String path, String createBody, String field, Object created, String patchBody, Object patched,
            boolean hasPatch, boolean hasDetail) {
        @Override
        public String toString() {
            return path;
        }
    }

    static Stream<Resource> resources() {
        return Stream.of(
            new Resource("/api/courses", "{\"name\":\"Docker\",\"image\":\"http://example.com/c.jpg\"}",
                "name", "Docker", "{\"name\":\"Docker expert\"}", "Docker expert", true, true),
            new Resource("/api/achievements", "{\"year\":2023,\"comment\":\"DevOps\",\"title\":\"Abstract\"}",
                "title", "Abstract", "{\"title\":\"Renamed\"}", "Renamed", true, false),
            new Resource("/api/projects",
                "{\"title\":\"portfolio\",\"description\":\"golang and react\",\"startDate\":\"2024-06-21T09:49:48.385Z\"}",
                "title", "portfolio", "{\"title\":\"portfolio v2\"}", "portfolio v2", true, true),
            new Resource("/api/galleries", "{\"image\":\"http://example.com/image_1.jpg\"}",
                "image", "http://example.com/image_1.jpg", null, null, false, false));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void ownerCreatesAndAnyoneListsByUsername(Resource r) throws Exception {
        Session owner = signup();
        int id = idOf(mvc.perform(as(post(r.path()).contentType(JSON).content(r.createBody()), owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$." + r.field()).value(r.created()))
            .andExpect(jsonPath("$.user").doesNotExist())
            .andReturn());

        mvc.perform(get(r.path()).param("username", owner.username()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(id));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void listingUnknownOrMissingUsernameIsEmpty(Resource r) throws Exception {
        mvc.perform(get(r.path()).param("username", "nobody-" + unique("")))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(r.path())).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void writesRequireAuthentication(Resource r) throws Exception {
        mvc.perform(post(r.path()).contentType(JSON).content(r.createBody())).andExpect(status().isUnauthorized());
        mvc.perform(delete(r.path() + "/1")).andExpect(status().isUnauthorized());
        if (r.hasPatch()) {
            mvc.perform(patch(r.path() + "/1").contentType(JSON).content(r.patchBody())).andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @MethodSource("resources")
    void invalidBodyIsBadRequest(Resource r) throws Exception {
        Session owner = signup();
        mvc.perform(as(post(r.path()).contentType(JSON).content("{}"), owner))
            .andExpect(status().isBadRequest());
        mvc.perform(as(post(r.path()).contentType(JSON).content("[1,2]"), owner))
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("resources")
    void ownerPatchesPartially(Resource r) throws Exception {
        if (!r.hasPatch()) {
            return;
        }
        Session owner = signup();
        int id = idOf(mvc.perform(as(post(r.path()).contentType(JSON).content(r.createBody()), owner)).andReturn());

        mvc.perform(as(patch(r.path() + "/" + id).contentType(JSON).content(r.patchBody()), owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$." + r.field()).value(r.patched()));

        // unknown fields (the go-server docs even send {"name":...} to achievements) and JSON nulls change nothing
        mvc.perform(as(patch(r.path() + "/" + id).contentType(JSON).content("{\"nope\":1,\"" + r.field() + "\":null}"), owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + r.field()).value(r.patched()));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void otherUsersGetNotFoundOnSomeoneElsesData(Resource r) throws Exception {
        Session owner = signup();
        Session intruder = signup();
        int id = idOf(mvc.perform(as(post(r.path()).contentType(JSON).content(r.createBody()), owner)).andReturn());

        mvc.perform(as(delete(r.path() + "/" + id), intruder)).andExpect(status().isNotFound());
        if (r.hasPatch()) {
            mvc.perform(as(patch(r.path() + "/" + id).contentType(JSON).content(r.patchBody()), intruder))
                .andExpect(status().isNotFound());
        }
        if (r.hasDetail()) {
            mvc.perform(as(get(r.path() + "/" + id), intruder)).andExpect(status().isNotFound());
            mvc.perform(as(get(r.path() + "/" + id), owner)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        }
        mvc.perform(get(r.path()).param("username", owner.username()))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0]." + r.field()).value(r.created()));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void ownerDeletesWith204ThenItIsGone(Resource r) throws Exception {
        Session owner = signup();
        int id = idOf(mvc.perform(as(post(r.path()).contentType(JSON).content(r.createBody()), owner)).andReturn());

        mvc.perform(as(delete(r.path() + "/" + id), owner)).andExpect(status().isNoContent());
        mvc.perform(as(delete(r.path() + "/" + id), owner)).andExpect(status().isNotFound());
        mvc.perform(get(r.path()).param("username", owner.username())).andExpect(jsonPath("$", hasSize(0)));
    }

    @ParameterizedTest
    @MethodSource("resources")
    void limitCapsTheListing(Resource r) throws Exception {
        Session owner = signup();
        for (int i = 0; i < 3; i++) {
            mvc.perform(as(post(r.path()).contentType(JSON).content(r.createBody()), owner)).andExpect(status().isOk());
        }
        mvc.perform(get(r.path()).param("username", owner.username()).param("limit", "2"))
            .andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get(r.path()).param("username", owner.username()).param("limit", "-1"))
            .andExpect(jsonPath("$", hasSize(3)));
    }

    // ---- bulk (courses and achievements)

    @Test
    void coursesBulkCreatesEveryItemAndAnswers204() throws Exception {
        Session owner = signup();
        mvc.perform(as(post("/api/courses").param("type", "bulk").contentType(JSON)
                .content("[{\"name\":\"Jenkins\",\"image\":\"a\"},{\"name\":\"Node.js\",\"image\":\"b\"}]"), owner))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/courses").param("username", owner.username()))
            .andExpect(jsonPath("$[*].name", contains("Jenkins", "Node.js")));
    }

    @Test
    void bulkRejectsNonArrayAndInvalidItemsAtomically() throws Exception {
        Session owner = signup();
        mvc.perform(as(post("/api/courses").param("type", "bulk").contentType(JSON).content("{\"name\":\"x\"}"), owner))
            .andExpect(status().isBadRequest());
        mvc.perform(as(post("/api/courses").param("type", "bulk").contentType(JSON)
                .content("[{\"name\":\"ok\",\"image\":\"a\"},{\"image\":\"missing name\"}]"), owner))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/courses").param("username", owner.username()))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void achievementsBulkThenListedByYearDescending() throws Exception {
        Session owner = signup();
        mvc.perform(as(post("/api/achievements").param("type", "bulk").contentType(JSON).content("""
                [{"year":2019,"comment":"c1","title":"BIDSS"},
                 {"year":2023,"comment":"c2","title":"Abstract"},
                 {"year":2021,"comment":"c3","title":"SignsCloud"}]"""), owner))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/achievements").param("username", owner.username()))
            .andExpect(jsonPath("$[*].year", contains(2023, 2021, 2019)));
    }

    @Test
    void achievementYearMustBePositive() throws Exception {
        Session owner = signup();
        mvc.perform(as(post("/api/achievements").contentType(JSON)
                .content("{\"year\":0,\"comment\":\"c\",\"title\":\"t\"}"), owner))
            .andExpect(status().isBadRequest());
    }

    @Test
    void projectsAreListedByStartDateDescendingWithIsoDates() throws Exception {
        Session owner = signup();
        for (String start : new String[] {"2022-01-01T00:00:00Z", "2024-06-21T09:49:48.385Z", "2023-03-03T00:00:00Z"}) {
            mvc.perform(as(post("/api/projects").contentType(JSON).content(
                    "{\"title\":\"p\",\"description\":\"d\",\"startDate\":\"%s\"}".formatted(start)), owner))
                .andExpect(status().isOk());
        }
        mvc.perform(get("/api/projects").param("username", owner.username()))
            .andExpect(jsonPath("$[*].startDate", contains(
                "2024-06-21T09:49:48.385Z", "2023-03-03T00:00:00Z", "2022-01-01T00:00:00Z")));
    }
}
