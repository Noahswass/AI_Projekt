package com.example.AI_Projekt.error;

import com.example.AI_Projekt.rebrickable.RebrickableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PB-21 for the JSON API: each kind of error becomes a ProblemDetail with the right status and
 * without any internals. The two controllers below exist only in this test.
 */
class GlobalExceptionHandlerTest {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingApi(), new FailingPage())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void anUnexpectedErrorIsA500WithoutInternals() throws Exception {
        mockMvc.perform(get("/test-api/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unexpected error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(content().string(not(containsString("secret detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void aRebrickableFailureIsA502() throws Exception {
        mockMvc.perform(get("/test-api/rebrickable"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Rebrickable is not available"))
                .andExpect(content().string(not(containsString("key=abc"))));
    }

    @Test
    void anInvalidBodyIsA400ThatNamesTheField() throws Exception {
        mockMvc.perform(post("/test-api/things").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Give it a name."));
    }

    @Test
    void anUnreadableBodyIsA400() throws Exception {
        mockMvc.perform(post("/test-api/things").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void anAccessErrorIsA403() throws Exception {
        mockMvc.perform(get("/test-api/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    void pagesAreNotAnsweredWithJson() {
        // The handler leaves @Controller classes alone, so the error travels on to Spring Boot's
        // error page. In this bare setup there is nothing behind it, so it surfaces as an exception.
        assertThatThrownBy(() -> mockMvc.perform(get("/test-page/boom")))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    record Thing(@NotBlank(message = "Give it a name.") String name) {
    }

    @RestController
    static class FailingApi {

        @GetMapping("/test-api/boom")
        String boom() {
            throw new IllegalStateException("secret detail");
        }

        @GetMapping("/test-api/rebrickable")
        String rebrickable() {
            throw new RebrickableException("GET /lego/sets/?key=abc failed", 503, null);
        }

        @GetMapping("/test-api/forbidden")
        String forbidden() {
            throw new AccessDeniedException("no");
        }

        @PostMapping("/test-api/things")
        String create(@Valid @RequestBody Thing thing) {
            return thing.name();
        }
    }

    @Controller
    static class FailingPage {

        @GetMapping("/test-page/boom")
        String boom() {
            throw new IllegalStateException("page failed");
        }
    }
}
