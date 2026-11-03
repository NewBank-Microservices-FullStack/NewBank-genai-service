package com.newbank.microservices.genai;

import java.util.concurrent.ConcurrentHashMap;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * #1: per-user rate limiting. One Resilience4j limiter per caller id
 * (JWT subject); the gateway enforces additional coarse limits.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    RateLimiterRegistry rateLimiterRegistry(
            @Value("${newbank.genai.rate-limit-per-minute:30}") int perMinute) {
        RateLimiterConfig cfg = RateLimiterConfig.custom()
            .limitForPeriod(perMinute)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ZERO)
            .build();
        return RateLimiterRegistry.of(cfg);
    }

    @Bean
    ConcurrentHashMap<String, RateLimiter> perUserLimiters(RateLimiterRegistry registry) {
        return new ConcurrentHashMap<>();
    }
}
