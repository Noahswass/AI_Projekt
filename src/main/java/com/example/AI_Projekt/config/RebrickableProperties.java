package com.example.AI_Projekt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Rebrickable API settings.
 *
 * @param maxAttempts total tries per request for 429 and 5xx responses
 * @param backoff     wait before the first retry; doubles on each further retry
 */
@ConfigurationProperties(prefix = "app.rebrickable")
public record RebrickableProperties(
        String baseUrl,
        String apiKey,
        Duration timeout,
        @DefaultValue("4") int maxAttempts,
        @DefaultValue("1s") Duration backoff) {
}
