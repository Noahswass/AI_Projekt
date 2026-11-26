package com.example.AI_Projekt.web.admin;

import com.example.AI_Projekt.service.sync.SyncRunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

/** PB-07: only an ADMIN can start a sync, the form needs CSRF, and a second start is refused. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:adminsync;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class AdminSyncControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    SyncRunService syncRunService;

    @Test
    void adminSeesTheSyncPage() throws Exception {
        when(syncRunService.recentRuns()).thenReturn(List.of());
        mockMvc.perform(get("/admin/sync").with(user("boss").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Start sync now")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void adminStartsASync() throws Exception {
        when(syncRunService.startInBackground(any())).thenReturn(true);
        mockMvc.perform(post("/admin/sync").with(user("boss").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/sync"))
                .andExpect(flash().attributeExists("message"));
        verify(syncRunService).startInBackground(any());
    }

    @Test
    void secondStartWhileRunningIsRefused() throws Exception {
        when(syncRunService.startInBackground(any())).thenReturn(false);
        mockMvc.perform(post("/admin/sync").with(user("boss").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/sync"))
                .andExpect(flash().attributeExists("warning"));
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(post("/admin/sync").with(user("boss").roles("ADMIN")))
                .andExpect(status().isForbidden());
        verify(syncRunService, never()).startInBackground(any());
    }

    @Test
    void normalUserIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/sync").with(user("anna").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/sync").with(user("anna").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        verify(syncRunService, never()).startInBackground(any());
    }

    @Test
    void visitorIsSentToLogin() throws Exception {
        // As a browser; without Accept: text/html, HTTP Basic answers 401 instead of redirecting
        mockMvc.perform(get("/admin/sync").accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
