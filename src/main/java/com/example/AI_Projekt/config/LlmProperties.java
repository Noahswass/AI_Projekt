package com.example.AI_Projekt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;


/**
 * Claude API settings.
 *
 * @param effort the effort for the model
 * @param maxTokens   the maximum number of tokens used
 */
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        String baseUrl,
        String apiKey,
        String model,
        Duration timeout,
        @DefaultValue("2") int maxAttempts,
        @DefaultValue("2s") Duration backoff,
        @DefaultValue("low") String effort,
        @DefaultValue("1000") int maxTokens) {
}