package com.chaoticteam.backend.acceptance;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ProfileAcceptanceTest extends AcceptanceTest {

    private static final String PHONE = "{\"phoneNumber\":\"87654321\",\"countryCode\":\"504\",\"whatsapp\":true}";

    @Test
    void patchAcceptsSnakeCaseAndCamelCaseAndKeepsUntouchedFields() throws Exception {
        Session user = signup();
        mvc.perform(as(patch("/api/profile").contentType(JSON).content("""
                {"first_name":"Alex","last_name":"Diaz","bio":"Software engineer","twitter":"al3x",
                 "specialties":"DevOps,Backend","telephone":null}"""), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value(user.username()))
            .andExpect(jsonPath("$.profile.firstName").value("Alex"))
            .andExpect(jsonPath("$.profile.lastName").value("Diaz"))
            .andExpect(jsonPath("$.profile.bio").value("Software engineer"))
            .andExpect(jsonPath("$.profile.specialties").value("DevOps,Backend"));

        mvc.perform(as(patch("/api/profile").contentType(JSON).content("{\"lastName\":\"Díaz\",\"github\":\"https://github.com/al3xdiaz\"}"), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profile.firstName").value("Alex"))
            .andExpect(jsonPath("$.profile.lastName").value("Díaz"))
            .andExpect(jsonPath("$.profile.twitter").value("al3x"))
            .andExpect(jsonPath("$.profile.github").value("https://github.com/al3xdiaz"));
    }

    @Test
    void patchCannotOverwriteIdOrTelephones() throws Exception {
        Session user = signup();
        mvc.perform(as(patch("/api/profile").contentType(JSON).content("{\"id\":99999,\"telephoneEntity\":[]}"), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profile.id").value(org.hamcrest.Matchers.not(99999)));
    }

    @Test
    void patchRequiresAuthentication() throws Exception {
        mvc.perform(patch("/api/profile").contentType(JSON).content("{\"bio\":\"x\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicProfileByUsernameHidesSecrets() throws Exception {
        Session user = signup();
        mvc.perform(get("/api/profile").param("username", user.username()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value(user.username()))
            .andExpect(jsonPath("$.profile.firstName").value("alex"))
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void unknownOrMissingUsernameIs404() throws Exception {
        mvc.perform(get("/api/profile").param("username", "ghost-" + unique("")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("user not found"));
        mvc.perform(get("/api/profile")).andExpect(status().isNotFound());
    }

    @Test
    void telephoneIsAddedToTheProfileAndCanBeDeleted() throws Exception {
        Session user = signup();
        int id = idOf(mvc.perform(as(post("/api/telephone").contentType(JSON).content(PHONE), user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phoneNumber").value("87654321"))
            .andExpect(jsonPath("$.countryCode").value("504"))
            .andExpect(jsonPath("$.whatsapp").value(true))
            .andReturn());

        mvc.perform(get("/api/profile").param("username", user.username()))
            .andExpect(jsonPath("$.profile.telephone", hasSize(1)))
            .andExpect(jsonPath("$.profile.telephone[0].id").value(id));

        mvc.perform(as(delete("/api/telephone/" + id), user)).andExpect(status().isNoContent());
        mvc.perform(as(delete("/api/telephone/" + id), user)).andExpect(status().isNotFound());
        mvc.perform(get("/api/profile").param("username", user.username()))
            .andExpect(jsonPath("$.profile.telephone", hasSize(0)));
    }

    @Test
    void telephoneValidationAndOwnership() throws Exception {
        Session owner = signup();
        Session intruder = signup();
        mvc.perform(post("/api/telephone").contentType(JSON).content(PHONE)).andExpect(status().isUnauthorized());
        mvc.perform(as(post("/api/telephone").contentType(JSON).content("{\"countryCode\":\"504\"}"), owner))
            .andExpect(status().isBadRequest());

        int id = idOf(mvc.perform(as(post("/api/telephone").contentType(JSON).content(PHONE), owner)).andReturn());
        mvc.perform(as(delete("/api/telephone/" + id), intruder)).andExpect(status().isNotFound());
        mvc.perform(get("/api/profile").param("username", owner.username()))
            .andExpect(jsonPath("$.profile.telephone", hasSize(1)));
    }

    @Test
    void vcardIsAnAttachmentBuiltFromTheProfile() throws Exception {
        Session user = signup();
        mvc.perform(as(patch("/api/profile").contentType(JSON).content(
                "{\"first_name\":\"Alex\",\"last_name\":\"Diaz\",\"photo\":\"http://x/p.jpg\",\"github\":\"gh\",\"website\":\"http://site\"}"), user));
        mvc.perform(as(post("/api/telephone").contentType(JSON).content(PHONE), user));

        mvc.perform(get("/api/vcard/" + user.username()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, startsWith("text/vcard")))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + user.username() + ".vcf"))
            .andExpect(content().string(startsWith("BEGIN:VCARD\r\nVERSION:2.1\r\n")))
            .andExpect(content().string(containsString("FN:Alex Diaz\r\n")))
            .andExpect(content().string(containsString("NICKNAME:" + user.username() + "\r\n")))
            .andExpect(content().string(containsString("TEL;CELL:+50487654321\r\n")))
            .andExpect(content().string(containsString("EMAIL:" + user.username() + "@example.io\r\n")))
            .andExpect(content().string(containsString("PHOTO;VALUE=URI:http://x/p.jpg\r\n")))
            .andExpect(content().string(containsString("X-SOCIALPROFILE;TYPE=github:gh\r\n")))
            .andExpect(content().string(containsString("URL:http://site\r\n")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("TYPE=twitter"))))
            .andExpect(content().string(org.hamcrest.Matchers.endsWith("END:VCARD\r\n")));
    }

    @Test
    void vcardIs404ForUnknownUsersAndUsersWithoutTelephone() throws Exception {
        Session withoutPhone = signup();
        mvc.perform(get("/api/vcard/" + withoutPhone.username()))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("the user not has telephone"));
        mvc.perform(get("/api/vcard/ghost-" + unique(""))).andExpect(status().isNotFound());
    }

    @Test
    void usersListsUsernamesPublicly() throws Exception {
        Session user = signup();
        mvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasItem(user.username())));
    }
}
