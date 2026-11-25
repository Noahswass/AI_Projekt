package com.example.AI_Projekt.web;

import com.example.AI_Projekt.service.RebrickableLinkService;
import com.example.AI_Projekt.service.RebrickableLoginFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PB-13: the settings page shows the link state, links and unlinks, and needs a login. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:accountsettings;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class AccountSettingsControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    RebrickableLinkService rebrickableLinkService;

    @Test
    void pageShowsNotLinked() throws Exception {
        // Arrange: the mock says anna has no link
        when(rebrickableLinkService.isLinked("anna")).thenReturn(false);
        //   template: AdminSyncControllerTest line 46 (when ... thenReturn)

        // Act + Assert: GET /account/settings as user("anna")
        mockMvc.perform(get("/account/settings").with(user("anna")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No Rebrickable account linked yet")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void pageShowsLinked() throws Exception {
        // Arrange: the mock says anna IS linked
        when(rebrickableLinkService.isLinked("anna")).thenReturn(true);

        // Act + Assert: same GET as above
        mockMvc.perform(get("/account/settings").with(user("anna")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your Rebrickable account is linked")));
    }

    @Test
    void linkSucceeds() throws Exception {
        // Arrange: nothing - a void method on a mock does nothing by default

        // Act + Assert: POST /account/settings/link as anna, with csrf()
        mockMvc.perform(post("/account/settings/link").with(user("anna")).with(csrf())
                .param("rebrickableUsername", "rbuser")
                .param("password", "pw"))
                .andExpect(redirectedUrl("/account/settings"))
                .andExpect(flash().attributeExists("success"));

        // Verify: the service got "anna", "rbuser", "pw" - in that order
        verify(rebrickableLinkService).linkAccount("anna", "rbuser", "pw");
        //   template: AdminSyncControllerTest line 59
    }

    @Test
    void wrongPasswordShowsError() throws Exception {
        // Arrange: make linkAccount throw RebrickableLoginFailedException
        doThrow(RebrickableLoginFailedException.class).when(rebrickableLinkService)
                .linkAccount("anna", "rbuser", "pw");

        // Act + Assert: the same POST as linkSucceeds
        //   expect a redirect and an "error" flash
        mockMvc.perform(post("/account/settings/link").with(user("anna")).with(csrf())
                .param("rebrickableUsername", "rbuser")
                .param("password", "pw"))
                .andExpect(redirectedUrl("/account/settings"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void visitorIsSentToLogin() throws Exception {
        // Act + Assert: GET /account/settings with NO user
        // As a browser; without Accept: text/html, HTTP Basic answers 401 instead of redirecting
        mockMvc.perform(get("/account/settings").accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
