package com.example.AI_Projekt.rebrickable;

import com.example.AI_Projekt.config.HTTPClientConfig;
import com.example.AI_Projekt.config.RebrickableProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.context.annotation.Condition;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RebrickableClientTest {

    private static final String THEMES = "/api/v3/lego/themes/";
    private static final String USER_TOKEN = "/api/v3/users/_token/";
    private static final String SETS = "/api/v3/lego/sets/";
    private static final String COLOURS = "/api/v3/lego/colors/";
    private static final String PARTS = "/api/v3/lego/parts/";
    private static final String MINIFIGS = "/api/v3/lego/minifigs/";
    private static final String EMPTY_LAST_PAGE =
            "{\"count\":0,\"next\":null,\"previous\":null,\"results\":[]}";
    private static final int MAX_ATTEMPTS = 3;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private RebrickableClient client;

    @BeforeEach
    void setUp() {
        var properties = new RebrickableProperties(
                wireMock.baseUrl() + "/api/v3", "test-key",
                Duration.ofMillis(3000), MAX_ATTEMPTS, Duration.ofMillis(10));
        RestClient restClient = new HTTPClientConfig(properties).rebrickableRestClient(RestClient.builder());
        client = new RebrickableClient(restClient, properties);
    }


    @Test
    void followsPaginationAndSendsApiKey() throws IOException {
        wireMock.stubFor(get(urlPathEqualTo(THEMES)).withQueryParam("page", equalTo("1"))
                .willReturn(okJson(fixture("themes-page1.json"))));
        wireMock.stubFor(get(urlPathEqualTo(THEMES)).withQueryParam("page", equalTo("2"))
                .willReturn(okJson("""
                        {"count":4,"next":null,"previous":"x",
                         "results":[{"id":5,"parent_id":null,"name":"Model"}]}""")));

        List<ThemeDto> themes = client.fetchThemes();

        assertThat(themes).extracting(ThemeDto::id).containsExactly(1, 3, 4, 5);
        assertThat(themes.get(0).parentId()).isNull();
        assertThat(themes.get(1)).isEqualTo(new ThemeDto(3, 1, "Competition"));
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(THEMES))
                .withHeader("Authorization", equalTo("key test-key"))
                .withQueryParam("page_size", equalTo(String.valueOf(RebrickableClient.PAGE_SIZE))));
    }

    @Test
    void mapsSetFields() throws IOException {
        wireMock.stubFor(get(urlPathEqualTo(SETS)).withQueryParam("page", equalTo("1"))
                .willReturn(okJson(fixture("sets-page1.json"))));
        wireMock.stubFor(get(urlPathEqualTo(SETS)).withQueryParam("page", equalTo("2"))
                .willReturn(okJson(EMPTY_LAST_PAGE)));

        List<SetDto> sets = client.fetchSets();

        assertThat(sets).hasSize(3);
        assertThat(sets.get(1)).isEqualTo(new SetDto(
                "001-1", "Gears", 1965, 756, 43,
                "https://cdn.rebrickable.com/media/sets/001-1/11530.jpg",
                "https://rebrickable.com/sets/001-1/gears/",
                Instant.parse("2024-01-08T19:22:27.085452Z")));
    }

    @Test
    void mapsColourFieldsAndIgnoresUnknownOnes() {
        wireMock.stubFor(get(urlPathEqualTo(COLOURS)).willReturn(okJson("""
                {"count":1,"next":null,"previous":null,
                 "results":[{"id":-1,"name":"[Unknown]","rgb":"0033B2","is_trans":true,
                             "external_ids":{"BrickOwl":{"ext_ids":[0]}}}]}""")));

        assertThat(client.fetchColours()).containsExactly(new ColourDto(-1, "[Unknown]", "0033B2", true));
    }

    @Test
    void unauthorizedFailsWithoutRetry() {
        wireMock.stubFor(get(urlPathEqualTo(THEMES))
                .willReturn(aResponse().withStatus(401).withBody("{\"detail\":\"Invalid token.\"}")));

        assertThatThrownBy(() -> client.fetchThemes())
                .isInstanceOf(RebrickableException.class)
                .satisfies(ex -> assertThat(((RebrickableException) ex).getStatus()).isEqualTo(401));
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(THEMES)));
    }

    @Test
    void userTokenSuccess() {
        wireMock.stubFor(post(urlPathEqualTo(USER_TOKEN))
                .willReturn(okJson("{\"user_token\":\"test_token\"}")));
        String token = client.fetchUserToken("anna","secret");
        assertThat(token)
                .isEqualTo("test_token");
        wireMock.verify(postRequestedFor(urlPathEqualTo(USER_TOKEN))
                .withRequestBody(equalTo("username=anna&password=secret"))
                .withHeader("Authorization", equalTo("key test-key")));
    }

    @Test
    void wrongRebrickablePwd() {
        wireMock.stubFor(post(urlPathEqualTo(USER_TOKEN))
                .willReturn(aResponse().withStatus(403).withBody("{\"detail\":\"Invalid credentials\"}")));
        RebrickableException ex = assertThrows(RebrickableException.class, () -> client.fetchUserToken("anna","wrong"));
        assertThat(ex.getStatus()).isEqualTo(403);
        wireMock.verify(1, postRequestedFor(urlPathEqualTo(USER_TOKEN))
                .withRequestBody(equalTo("username=anna&password=wrong"))
                .withHeader("Authorization", equalTo("key test-key")));
    }

    @Test
    void emptyBodyFailsWithRebrickableException() {
        wireMock.stubFor(get(urlPathEqualTo(THEMES))
                .willReturn(okJson("")));

        assertThatThrownBy(() -> client.fetchThemes())
                .isInstanceOf(RebrickableException.class)
                .hasMessageContaining("empty body");
        wireMock.verify(1, getRequestedFor(urlPathEqualTo(THEMES)));
    }

    @Test
    void tooManyRequestsIsRetriedThenSucceeds() {
        wireMock.stubFor(get(urlPathEqualTo(THEMES)).inScenario("throttled")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "0"))
                .willSetStateTo("recovered"));
        wireMock.stubFor(get(urlPathEqualTo(THEMES)).inScenario("throttled")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson("""
                        {"count":1,"next":null,"previous":null,
                         "results":[{"id":1,"parent_id":null,"name":"Technic"}]}""")));

        assertThat(client.fetchThemes()).containsExactly(new ThemeDto(1, null, "Technic"));
        wireMock.verify(2, getRequestedFor(urlPathEqualTo(THEMES)));
    }

    @Test
    void serverErrorIsRetriedUntilAttemptsAreUsedUp() {
        wireMock.stubFor(get(urlPathEqualTo(THEMES)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.fetchThemes())
                .isInstanceOf(RebrickableException.class)
                .satisfies(ex -> assertThat(((RebrickableException) ex).getStatus()).isEqualTo(503));
        wireMock.verify(MAX_ATTEMPTS, getRequestedFor(urlPathEqualTo(THEMES)));
    }

    @Test
    void timeoutFailsWithRebrickableException() {
        var properties = new RebrickableProperties(
                wireMock.baseUrl() + "/api/v3", "test-key",
                Duration.ofMillis(200), MAX_ATTEMPTS, Duration.ofMillis(10));
        RestClient restClient = new HTTPClientConfig(properties).rebrickableRestClient(RestClient.builder());
        var impatientClient = new RebrickableClient(restClient, properties);

        wireMock.stubFor(get(urlPathEqualTo(THEMES))
                .willReturn(okJson(EMPTY_LAST_PAGE).withFixedDelay(500)));

        assertThatThrownBy(() -> impatientClient.fetchThemes())
                .isInstanceOf(RebrickableException.class)
                .hasMessageContaining("did not respond")
                .satisfies(ex -> assertThat(((RebrickableException) ex).getStatus()).isNull());
    }

    private static String fixture(String name) throws IOException {
        try (InputStream in = RebrickableClientTest.class.getResourceAsStream("/fixtures/" + name)) {
            assertThat(in).as("fixture %s", name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
