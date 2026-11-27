package com.example.AI_Projekt.service.ai;

import com.example.AI_Projekt.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test: no Spring, the limiter is built by hand.
 * Anonymous gets 1 question, logged in gets 3, and the refill is long
 * enough (1 hour) that no token comes back during the test.
 */
class AssistantRateLimiterTest {

    private AssistantRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new AssistantRateLimiter(new RateLimitProperties(1, 3, Duration.ofHours(1)));
    }

    @Test
    void anonymousVisitorGetsOneQuestionThenIsBlocked() {
        assertThat(limiter.tryConsume("ip:10.0.0.5", false)).isTrue();
        assertThat(limiter.tryConsume("ip:10.0.0.5", false)).isFalse();
    }

    @Test
    void loggedInUserGetsThreeQuestionsThenIsBlocked() {
        assertThat(limiter.tryConsume("user:noah", true)).isTrue();
        assertThat(limiter.tryConsume("user:noah", true)).isTrue();
        assertThat(limiter.tryConsume("user:noah", true)).isTrue();
        assertThat(limiter.tryConsume("user:noah", true)).isFalse();
    }

    @Test
    void differentKeysDoNotShareABucket() {
        assertThat(limiter.tryConsume("ip:10.0.0.5", false)).isTrue();
        assertThat(limiter.tryConsume("ip:10.0.0.6", false)).isTrue();   // new visitor, full bucket
        assertThat(limiter.tryConsume("ip:10.0.0.5", false)).isFalse();  // first one is still empty
    }
}
