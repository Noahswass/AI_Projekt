package com.example.AI_Projekt.rebrickable;

import com.example.AI_Projekt.config.RebrickableProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the Rebrickable catalog. Each method follows pagination and returns all rows.
 * 429 and 5xx responses are retried with exponential backoff; other errors fail at once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RebrickableClient {

    static final int PAGE_SIZE = 1000;
    private static final Duration MAX_WAIT = Duration.ofSeconds(60);

    private final RestClient rebrickableRestClient;
    private final RebrickableProperties properties;

    public List<ThemeDto> fetchThemes() {
        return fetchAll("/lego/themes/", new ParameterizedTypeReference<>() {
        });
    }

    public List<ColourDto> fetchColours() {
        return fetchAll("/lego/colors/", new ParameterizedTypeReference<>() {
        });
    }

    public List<SetDto> fetchSets() {
        return fetchAll("/lego/sets/", new ParameterizedTypeReference<>() {
        });
    }

    public List<PartDto> fetchParts() {
        return fetchAll("/lego/parts/", new ParameterizedTypeReference<>() {
        });
    }

    public List<MinifigDto> fetchMinifigs() {
        return fetchAll("/lego/minifigs/", new ParameterizedTypeReference<>() {
        });
    }

    private <T> List<T> fetchAll(String path, ParameterizedTypeReference<PageResponse<T>> type) {
        List<T> all = new ArrayList<>();
        int page = 1;
        PageResponse<T> response;
        do {
            response = fetchPage(path, page, type);
            if (response == null || response.results() == null) {
                throw new RebrickableException("Rebrickable returned an empty body for " + path, null, null);
            }
            all.addAll(response.results());
            page++;
        } while (response.next() != null);
        log.debug("Fetched {} rows from {} in {} page(s)", all.size(), path, page - 1);
        return all;
    }

    public String fetchUserToken(String username, String password) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);
        try {
            UserTokenDto dto = rebrickableRestClient.post()
                    .uri("/users/_token/")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(UserTokenDto.class);
            if (dto == null || dto.userToken() == null) {
                throw new RebrickableException("Rebrickable returned no token", 200, null);
            }
            return dto.userToken();
        } catch (ResourceAccessException ex) {
            throw new RebrickableException("Rebrickable did not respond for /users/_token/", null, ex);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            throw new RebrickableException(
                    "Rebrickable returned " + status + " for /users/_token/",
                    status, ex);
        }
    }

    private <T> PageResponse<T> fetchPage(String path, int page, ParameterizedTypeReference<PageResponse<T>> type) {
        Duration backoff = properties.backoff();
        for (int attempt = 1; ; attempt++) {
            try {
                return rebrickableRestClient.get()
                        .uri(uri -> uri.path(path)
                                .queryParam("page", page)
                                .queryParam("page_size", PAGE_SIZE)
                                .build())
                        .retrieve()
                        .body(type);
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= properties.maxAttempts()) {
                    throw new RebrickableException(
                            "Rebrickable returned " + status + " for " + path + " (attempt " + attempt + ")",
                            status, ex);
                }
                Duration wait = retryAfter(ex, backoff);
                log.warn("Rebrickable returned {} for {}, retrying in {} (attempt {}/{})",
                        status, path, wait, attempt, properties.maxAttempts());
                sleep(wait);
                backoff = backoff.multipliedBy(2);
            } catch (ResourceAccessException ex) {
                throw new RebrickableException("Rebrickable did not respond for " + path, null, ex);
            }
        }
    }

    /** Uses the server's Retry-After (seconds) when present, otherwise the current backoff. */
    private Duration retryAfter(RestClientResponseException ex, Duration backoff) {
        HttpHeaders headers = ex.getResponseHeaders();
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        Duration wait = backoff;
        if (value != null) {
            try {
                wait = Duration.ofSeconds(Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // HTTP-date form: fall back to the backoff
            }
        }
        return wait.compareTo(MAX_WAIT) > 0 ? MAX_WAIT : wait;
    }

    private void sleep(Duration wait) {
        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RebrickableException("Interrupted while waiting to retry Rebrickable", null, ex);
        }
    }
}
