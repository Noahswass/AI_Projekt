package com.example.AI_Projekt.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PB-21 for pages: a browser gets the friendly error page, not JSON and not a stack trace.
 *
 * This starts a real server on a random port. MockMvc cannot test this, because it stops at the
 * first answer and never makes the second, internal request to /error that produces the page.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:errorpages;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class ErrorPagesTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void anUnknownAddressGivesAFriendly404Page() {
        ResponseEntity<String> response = asBrowser(HttpMethod.GET, "/catalog/no-such-page");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        assertThat(response.getBody())
                .contains("We could not find that page")
                .contains("href=\"/\"")
                .doesNotContain("Whitelabel")
                .doesNotContain("Exception")
                .doesNotContain("\tat ");
    }

    @Test
    void aFormWithoutCsrfTokenGivesAFriendly403Page() {
        ResponseEntity<String> response = asBrowser(HttpMethod.POST, "/register");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .contains("You are not allowed to do that")
                .doesNotContain("Exception");
    }

    private ResponseEntity<String> asBrowser(HttpMethod method, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.TEXT_HTML));
        HttpEntity<?> request = method == HttpMethod.POST
                ? new HttpEntity<>(new LinkedMultiValueMap<String, String>(), formHeaders(headers))
                : new HttpEntity<>(headers);
        return rest.exchange(path, method, request, String.class);
    }

    private HttpHeaders formHeaders(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }
}
