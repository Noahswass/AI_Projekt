package com.example.AI_Projekt.web;

import com.example.AI_Projekt.entity.AppUser;
import com.example.AI_Projekt.entity.Role;
import com.example.AI_Projekt.repository.AppUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PB-11: register, log in, log out and the access rules, through the whole app on H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:accounts;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class AccountFlowTest {

    // Made up for these tests; exists nowhere else
    private static final String PASSWORD = "brick-by-brick-42";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    AppUserRepo appUserRepo;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void emptyUserTable() {
        appUserRepo.deleteAll();
    }

    // --- Register ---

    @Test
    void registerAndLoginPagesAreOpenAndCarryACsrfToken() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void registeringStoresABcryptHashAndSendsTheUserToLogin() throws Exception {
        register("anna", PASSWORD, PASSWORD)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        // The page it redirects to must be open to a visitor and say that the account exists
        mockMvc.perform(get("/login?registered").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Your account is created.")));

        AppUser saved = appUserRepo.findByUsernameIgnoreCase("anna").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2").hasSize(60).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    void registeringWithoutACsrfTokenIsRefused() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "anna")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isForbidden());

        assertThat(appUserRepo.count()).isZero();
    }

    @Test
    void aTooShortPasswordShowsTheFormAgainWithAMessage() throws Exception {
        register("anna", "short", "short")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The password must be 8 to 72 characters.")))
                // the username is kept so the user does not have to retype it, the password is not
                .andExpect(content().string(containsString("value=\"anna\"")))
                .andExpect(content().string(not(containsString("value=\"short\""))));

        assertThat(appUserRepo.count()).isZero();
    }

    @Test
    void twoDifferentPasswordsAreRefused() throws Exception {
        register("anna", PASSWORD, PASSWORD + "x")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The two passwords are not the same.")));

        assertThat(appUserRepo.count()).isZero();
    }

    @Test
    void aUsernameWithOtherCharactersIsRefused() throws Exception {
        register("anna <script>", PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Use only letters, digits, dot, dash and underscore.")));

        assertThat(appUserRepo.count()).isZero();
    }

    @Test
    void aTakenUsernameIsRefusedAlsoInAnotherCase() throws Exception {
        register("anna", PASSWORD, PASSWORD).andExpect(status().is3xxRedirection());

        register("ANNA", PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("That username is already taken.")));

        assertThat(appUserRepo.count()).isEqualTo(1);
    }

    @Test
    void theAdminUsernameCannotBeRegisteredInAnyCase() throws Exception {
        // PB-12: app.admin.username is test_admin in application-test.properties. No admin row exists
        // here (the password is blank in tests), so only the reserved name can stop this.
        register("TEST_Admin", PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("That username is already taken.")));

        assertThat(appUserRepo.count()).isZero();
    }

    // --- Log in and out ---

    @Test
    void aNewAccountCanLogInAtOnce() throws Exception {
        register("anna", PASSWORD, PASSWORD);

        mockMvc.perform(formLogin("/login").user("anna").password(PASSWORD))
                .andExpect(authenticated().withUsername("anna").withRoles("USER"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void aWrongPasswordIsRefused() throws Exception {
        register("anna", PASSWORD, PASSWORD);

        mockMvc.perform(formLogin("/login").user("anna").password("not-the-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void anUnknownUserGetsTheSameAnswerAsAWrongPassword() throws Exception {
        mockMvc.perform(formLogin("/login").user("nobody").password(PASSWORD))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void aDisabledAccountCannotLogIn() throws Exception {
        register("anna", PASSWORD, PASSWORD);
        AppUser anna = appUserRepo.findByUsernameIgnoreCase("anna").orElseThrow();
        anna.setEnabled(false);
        appUserRepo.save(anna);

        mockMvc.perform(formLogin("/login").user("anna").password(PASSWORD))
                .andExpect(unauthenticated());
    }

    @Test
    void loggingOutEndsTheSessionAndGoesToTheFrontPage() throws Exception {
        // As a browser (accepts HTML); other clients get an empty 204 instead of a redirect
        mockMvc.perform(post("/logout").with(csrf()).with(user("anna")).accept(MediaType.TEXT_HTML))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/"));
    }

    // --- Access rules ---

    @Test
    void myPageSendsAVisitorToLogin() throws Exception {
        // A browser says it accepts HTML; that is what makes the answer a redirect and not a 401
        mockMvc.perform(get("/me").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/me/sets").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void myPageShowsTheNameOfTheLoggedInUser() throws Exception {
        mockMvc.perform(get("/me").with(user("anna")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("anna")));
    }

    @Test
    void frontPageOffersLoginToAVisitorAndLogoutToAUser() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(containsString("href=\"/register\"")))
                .andExpect(content().string(not(containsString("Log out"))));
        mockMvc.perform(get("/").with(user("anna")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Logged in as")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(not(containsString("href=\"/register\""))));
    }

    private ResultActions register(String username, String password, String confirmPassword) throws Exception {
        return mockMvc.perform(post("/register").with(csrf())
                .param("username", username)
                .param("password", password)
                .param("confirmPassword", confirmPassword));
    }
}
