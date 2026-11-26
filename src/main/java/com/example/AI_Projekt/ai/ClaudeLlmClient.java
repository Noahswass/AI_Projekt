package com.example.AI_Projekt.ai;

import com.example.AI_Projekt.config.LlmProperties;
import com.example.AI_Projekt.rebrickable.RebrickableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeLlmClient implements LlmClient {

    private final RestClient llmRestClient;
    private final LlmProperties llmProperties;
    private static final Duration MAX_WAIT = Duration.ofSeconds(2);

    @Override
    public String answer(String systemPrompt, String question) {
        RequestDto requestDto = new RequestDto(llmProperties.model(), llmProperties.maxTokens(), systemPrompt,
                List.of(new MessageDto("user", question)));
        Duration backoff = llmProperties.backoff();
        for (int attempt = 1; ; attempt++) {
            try {
                ResponseDto response = llmRestClient.post()
                        .uri("/v1/messages")
                        .body(requestDto)
                        .retrieve()
                        .body(ResponseDto.class);
                if (response == null || response.content() == null || response.content().isEmpty()) {
                    throw new LlmException("Anthropic returned null for /v1/messages",200,null);
                }
                return response.content().get(0).text();

            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= llmProperties.maxAttempts()) {
                    throw new LlmException(
                            "Anthropic returned " + status + " for /v1/messages (attempt " + attempt + ")",
                            status, ex);
                }
                Duration wait = retryAfter(ex, backoff);
                log.warn("Anthropic returned {} for /v1/messages, retrying in {} (attempt {}/{})",
                status, wait, attempt, llmProperties.maxAttempts());
                sleep(wait);
                backoff = backoff.multipliedBy(2);
            } catch (RestClientException ex) {
                throw new LlmException("Anthropic did not respond for /v1/messages", null, ex);
            }
        }
    }

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
            throw new LlmException("Interrupted while waiting to retry Anthropic", null, ex);
        }
    }
}

