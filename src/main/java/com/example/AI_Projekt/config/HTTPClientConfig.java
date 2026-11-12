package com.example.AI_Projekt.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The type Http client config.
 */
@Configuration
@RequiredArgsConstructor
public class HTTPClientConfig {

    private final RebrickableProperties rebrickable;

    /**
     * Rebrickable rest client rest client.
     *
     * @param builder the builder
     * @return the rest client
     */
    @Bean
    RestClient rebrickableRestClient(RestClient.Builder builder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(rebrickable.timeout());
        factory.setReadTimeout(rebrickable.timeout());
        return builder
                .baseUrl(rebrickable.baseUrl())
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "key " + rebrickable.apiKey())
                .build();
    }
}