package com.example.AI_Projekt.web.ai;

import com.example.AI_Projekt.service.ai.AssistantRateLimiter;
import com.example.AI_Projekt.service.ai.AssistantService;
import com.example.AI_Projekt.service.ai.AssistantService.AssistantAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.mock.web.MockHttpServletRequest;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PB-17: a visitor can open /assistant, ask a question and see the answer with linked sources. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:assistantcontroller;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class AssistantControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockBean
    AssistantService assistantService;
    @MockBean
    AssistantRateLimiter rateLimiter;     // one real bucket would leak between test methods

    @BeforeEach
    void allowByDefault() {
        when(rateLimiter.tryConsume(any(), anyBoolean())).thenReturn(true);
    }

    @Test
    void visitorCanOpenThePage() throws Exception {
        mockMvc.perform(get("/assistant"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"question\"")));
    }

    @Test
    void pageUsesTheSiteLayout() throws Exception {
        mockMvc.perform(get("/assistant"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("site-plate")))
                .andExpect(content().string(containsString("<title>")));
    }

    @Test
    void visitorSeesAnswerAndLinkedSources() throws Exception {
        when(assistantService.ask("Which Falcon sets exist?"))
                .thenReturn(new AssistantAnswer("The Millennium Falcon.", List.of("75192-1")));

        mockMvc.perform(post("/assistant").with(csrf()).param("question", "Which Falcon sets exist?"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The Millennium Falcon.")))
                .andExpect(content().string(containsString("href=\"/catalog/sets/75192-1\"")))
                .andExpect(content().string(containsString("Which Falcon sets exist?")));
    }

    @Test
    void noSourcesListWhenThereAreNoSources() throws Exception {
        when(assistantService.ask(any())).thenReturn(new AssistantAnswer("I don't know.", List.of()));

        mockMvc.perform(post("/assistant").with(csrf()).param("question", "Anything?"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/catalog/sets/"))));
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/assistant").param("question", "Falcon?"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(assistantService);
    }

    @Test
    void tooLongQuestionIsCutTo500Characters() throws Exception {
        when(assistantService.ask(any())).thenReturn(new AssistantAnswer("ok", List.of()));

        mockMvc.perform(post("/assistant").with(csrf()).param("question", "x".repeat(800)))
                .andExpect(status().isOk());

        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(assistantService).ask(sent.capture());
        assertThat(sent.getValue()).hasSize(500);
    }

    @Test
    void blankQuestionStillShowsThePage() throws Exception {
        mockMvc.perform(post("/assistant").with(csrf()).param("question", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Please enter a question before asking.")));

        verifyNoInteractions(assistantService, rateLimiter);   // blank: no Claude call, no token spent
    }

    // PB-19 step 4: the limiter guards the Claude call

    @Test
    void blockedVisitorIsNotSentToTheAssistant() throws Exception {
        when(rateLimiter.tryConsume(any(), anyBoolean())).thenReturn(false);

        mockMvc.perform(post("/assistant").with(csrf()).param("question", "Falcon?"))
                .andExpect(status().isTooManyRequests())          // PB-19 step 5: 429, still the page
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("question", "Falcon?"));

        verifyNoInteractions(assistantService);
    }

    @Test
    void allowedVisitorIsSentToTheAssistant() throws Exception {
        when(assistantService.ask("Falcon?")).thenReturn(new AssistantAnswer("ok", List.of()));

        mockMvc.perform(post("/assistant").with(csrf()).param("question", "Falcon?"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"));

        verify(rateLimiter).tryConsume(startsWith("ip:"), eq(false));
        verify(assistantService).ask("Falcon?");
    }

    // PB-19 step 3: the rate-limit key for the visitor

    @Test
    void visitorKeyUsesTheUsernameWhenLoggedIn() {
        Principal noah = () -> "noah";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        assertThat(AssistantController.visitorKey(noah, request)).isEqualTo("user:noah");
    }

    @Test
    void visitorKeyUsesTheIpForAnonymousVisitors() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        assertThat(AssistantController.visitorKey(null, request)).isEqualTo("ip:10.0.0.5");
    }
}
