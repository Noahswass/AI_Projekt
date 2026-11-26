package com.example.AI_Projekt.ai;

import com.example.AI_Projekt.config.HTTPClientConfig;
import com.example.AI_Projekt.config.LlmProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClaudeLlmClientTest {

    private static final String MESSAGES = "/v1/messages";        // the path your client POSTs to
    private static final int MAX_ATTEMPTS = (3);          // 3 makes "tried N times" easy to see

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();              // copy from RebrickableClientTest

    // Only for the timeout test, so its late request never counts in another test
    @RegisterExtension
    static WireMockExtension slowWireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private ClaudeLlmClient client;

    @BeforeEach
    void setUp() {
        // LlmProperties order: baseUrl, apiKey, model, timeout, maxAttempts, backoff, effort, maxTokens
        var properties = new LlmProperties(wireMock.baseUrl(), "test-key", "test-model",
                Duration.ofMillis(3000), MAX_ATTEMPTS, Duration.ofMillis(0), "low", 100);
        RestClient restClient = new HTTPClientConfig().llmRestClient(RestClient.builder(), properties);
        client = new ClaudeLlmClient(restClient, properties);
    }

    @Test
    void returnsTextFromResponse() throws IOException {
        // Arrange:
        wireMock.stubFor(post(urlPathEqualTo(MESSAGES)).willReturn(okJson(fixture("llm-message.json"))));
        // Act:
        String answer = client.answer("rules", "hello");
        // Assert:
        assertThat(answer).isEqualTo("Hi there! How can I help you today?");
    }

    @Test
    void sendsKeyVersionAndBody() throws IOException {
        // Arrange:
        wireMock.stubFor(post(urlPathEqualTo(MESSAGES)).willReturn(okJson(fixture("llm-message.json"))));
        // Act:
        client.answer("rules", "which castle sets?");
        // Assert:
        wireMock.verify(postRequestedFor(urlPathEqualTo(MESSAGES))
                .withHeader("x-api-key", equalTo("test-key"))
                .withHeader("anthropic-version", equalTo("2023-06-01"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("test-model")))
                .withRequestBody(matchingJsonPath("$.messages[0].content",
                        equalTo("which castle sets?"))));
    }

    @Test
    void unauthorizedThrowsWithoutRetry() {
        // Arrange:
        wireMock.stubFor(post(urlPathEqualTo(MESSAGES)).willReturn(aResponse().withStatus(401) ));
        // Act+Assert:
        assertThatThrownBy(() -> client.answer("rules", "hi"))
                   .isInstanceOf(LlmException.class)
                .extracting("status").isEqualTo(401);
        // Assert:
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(MESSAGES)));
    }

    @Test
    void tooManyRequestsIsRetriedThenSucceeds() throws IOException {
        // Arrange:
        // TWO stubs in a Scenario (find inScenario in RebrickableClientTest):
        //   1st:
        wireMock.stubFor(post(urlPathEqualTo(MESSAGES)).inScenario("busy")
           .inScenario("busy").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429)).willSetStateTo("ok"));
        //   2nd:
        wireMock.stubFor(post(urlPathEqualTo(MESSAGES)).inScenario("busy")
           .inScenario("busy").whenScenarioStateIs("ok")
               .willReturn(okJson(fixture("llm-message.json"))));
        // Act:
         String answer = client.answer("news","What is the newest LEGO set?");
        // Assert:
        // the answer is the fixture text, AND verify(___, postRequestedFor(...))
        assertThat(answer).isEqualTo("Hi there! How can I help you today?");
        wireMock.verify(2, postRequestedFor(urlPathEqualTo(MESSAGES)));
    }

    @Test
    void serverErrorIsRetriedThenGivesUp() {
        // Arrange: 503 every time (503, not 529: only "status >= 500" retries it)
        wireMock.stubFor(post(urlPathEqualTo(MESSAGES)).willReturn(aResponse().withStatus(503)));
        // Act+Assert: gives up after the last attempt and keeps the status
        assertThatThrownBy(() -> client.answer("rules", "hi"))
                .isInstanceOf(LlmException.class)
                .extracting("status").isEqualTo(503);
        // Assert: tried exactly MAX_ATTEMPTS times
        wireMock.verify(MAX_ATTEMPTS, postRequestedFor(urlPathEqualTo(MESSAGES)));
    }

    @Test
    void emptyContentThrows() {
        // Arrange: a 200 reply with no text blocks
        wireMock.stubFor(post(urlPathEqualTo(MESSAGES))
                .willReturn(okJson("{\"content\":[],\"stop_reason\":\"end_turn\"}")));
        // Act+Assert: an LlmException, not an IndexOutOfBoundsException from get(0)
        assertThatThrownBy(() -> client.answer("rules", "hi"))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void timeoutThrowsWithNullStatus() throws IOException {
        // Arrange: a second client with a short timeout, so only this test waits.
        // It talks to its own server: the slow request finishes after the test ends,
        // and on the shared server it would show up in the next test's request count.
        var properties = new LlmProperties(slowWireMock.baseUrl(), "test-key", "test-model",
                Duration.ofMillis(200), MAX_ATTEMPTS, Duration.ofMillis(0), "low", 100);
        RestClient restClient = new HTTPClientConfig().llmRestClient(RestClient.builder(), properties);
        var impatientClient = new ClaudeLlmClient(restClient, properties);
        slowWireMock.stubFor(post(urlPathEqualTo(MESSAGES))
                .willReturn(okJson(fixture("llm-message.json")).withFixedDelay(500)));
        // Act+Assert: no response at all, so there is no status
        assertThatThrownBy(() -> impatientClient.answer("rules", "hi"))
                .isInstanceOf(LlmException.class)
                .extracting("status").isNull();
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = ClaudeLlmClientTest.class.getResourceAsStream("/fixtures/" + name)) {
            assertThat(in).as("fixture %s", name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
