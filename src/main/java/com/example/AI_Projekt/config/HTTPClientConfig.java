package com.example.AI_Projekt.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The type Http client config.
 */
@Configuration
@RequiredArgsConstructor
public class HTTPClientConfig {


    /**
     * Rebrickable rest client rest client.
     *
     * @param builder the builder
     * @return the rest client
     */
    @Bean
    public RestClient rebrickableRestClient(RestClient.Builder builder, RebrickableProperties rebrickable) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(rebrickable.timeout());
        factory.setReadTimeout(rebrickable.timeout());
        return builder
                .baseUrl(rebrickable.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "key " + rebrickable.apiKey())
                .build();
    }
    @Bean
    public RestClient llmRestClient(RestClient.Builder builder, LlmProperties llm) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(llm.timeout());
        factory.setReadTimeout(llm.timeout());
        return builder
                .baseUrl(llm.baseUrl())
                .requestFactory(factory)
                .defaultHeader("x-api-key", llm.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}