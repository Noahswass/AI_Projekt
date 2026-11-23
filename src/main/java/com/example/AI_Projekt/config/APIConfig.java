package com.example.AI_Projekt.config;


import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class APIConfig implements ApplicationRunner {
    private final RebrickableProperties rebrickableProperties;

    public APIConfig(RebrickableProperties rebrickableProperties) {
        this.rebrickableProperties = rebrickableProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (rebrickableProperties.apiKey().isBlank()) {
            throw new IllegalStateException("Rebrickable API Key is missing");
        }
    }
}
