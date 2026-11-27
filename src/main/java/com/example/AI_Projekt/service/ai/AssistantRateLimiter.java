package com.example.AI_Projekt.service.ai;

import com.example.AI_Projekt.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AssistantRateLimiter {
    private final RateLimitProperties rateLimitProperties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket(int capacity){
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.simple(capacity, rateLimitProperties.refillTime()))
                .build();
        return bucket;
    }
    public boolean tryConsume(String key,boolean loggedIn) {
        int capacity = loggedIn ? rateLimitProperties.maxUserQuestions() : rateLimitProperties.maxNonUserQuestions();
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(capacity));
        return bucket.tryConsume(1);
    }
}
