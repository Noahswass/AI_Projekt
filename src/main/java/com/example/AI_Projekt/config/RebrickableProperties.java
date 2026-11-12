package com.example.AI_Projekt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/**
 * The type Rebrickable properties.
 */
@ConfigurationProperties(prefix = "app.rebrickable")
public record RebrickableProperties(String baseUrl, String apiKey, Duration timeout) {
}