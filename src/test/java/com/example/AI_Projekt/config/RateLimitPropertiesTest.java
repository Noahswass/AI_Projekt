package com.example.AI_Projekt.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Binds app.rate-limit.* the same way Spring Boot does at startup,
 * without starting the whole application.
 */
class RateLimitPropertiesTest {

    @Test
    void readsValuesFromApplicationProperties() throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new ResourcePropertySource("classpath:application.properties"));

        RateLimitProperties props = Binder.get(env)
                .bind("app.rate-limit", Bindable.of(RateLimitProperties.class))
                .get();

        assertThat(props.maxNonUserQuestions()).isEqualTo(1);
        assertThat(props.maxUserQuestions()).isEqualTo(3);
        assertThat(props.refillTime()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void usesDefaultsWhenPropertiesAreMissing() {
        RateLimitProperties props = Binder.get(new StandardEnvironment())
                .bindOrCreate("app.rate-limit", Bindable.of(RateLimitProperties.class));

        assertThat(props.maxNonUserQuestions()).isEqualTo(1);
        assertThat(props.maxUserQuestions()).isEqualTo(3);
        assertThat(props.refillTime()).isEqualTo(Duration.ofSeconds(30));
    }
}
