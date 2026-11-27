package com.example.AI_Projekt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Ip-Rate limit settings.
 * @param maxNonUserQuestions the maximum number of non-user questions allowed in a given time frame
 * @param maxUserQuestions the maximum number of user questions allowed in a given time frame
 * @param refillTime the time frame in which the bucket is refilled
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("1") int maxNonUserQuestions,
        @DefaultValue("3") int maxUserQuestions,
        @DefaultValue("30s") Duration refillTime
) {
}
