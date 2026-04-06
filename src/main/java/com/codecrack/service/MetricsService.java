package com.codecrack.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom Metrics Service for monitoring
 * Only loads when Redis and RabbitMQ are available
 */
@Service
@ConditionalOnBean({RedisTemplate.class, RabbitTemplate.class})
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter submissionsTotal;
    private final Counter submissionsAccepted;

    // Gauges
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.submissionsTotal = Counter.builder("submissions.total")
                .description("Total submissions received")
                .tag("type", "code")
                .register(meterRegistry);

        this.submissionsAccepted = Counter.builder("submissions.accepted")
                .description("Accepted submissions")
                .tag("verdict", "ACCEPTED")
                .register(meterRegistry);

        Gauge.builder("users.active", activeUsers, AtomicInteger::get)
                .description("Currently active users")
                .register(meterRegistry);
    }

    public void recordSubmission() {
        submissionsTotal.increment();
    }

    public void recordSubmissionAccepted() {
        submissionsAccepted.increment();
    }

    public void recordSubmissionRejected(String verdict) {
        Counter.builder("submissions.rejected")
                .tag("verdict", verdict)
                .register(meterRegistry)
                .increment();
    }

    public void recordExecutionTime(long milliseconds, String language) {
        Timer.builder("code.execution.time")
                .tag("language", language)
                .register(meterRegistry)
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void recordRateLimitExceeded(String userId, String type) {
        Counter.builder("rate_limit.exceeded")
                .tag("user_id", userId)
                .tag("type", type)
                .register(meterRegistry)
                .increment();
        log.warn("Rate limit exceeded: user={}, type={}", userId, type);
    }

    public void recordAuthFailure(String reason) {
        Counter.builder("auth.failures")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordRequestLatency(long milliseconds, String endpoint) {
        Timer.builder("api.request.latency")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void recordDatabaseQuery(long milliseconds, String operation) {
        Timer.builder("database.query.time")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void incrementActiveUsers() {
        activeUsers.incrementAndGet();
    }

    public void decrementActiveUsers() {
        activeUsers.decrementAndGet();
    }

    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hit")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.miss")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    public void recordError(String errorType, String message) {
        Counter.builder("errors.total")
                .tag("type", errorType)
                .register(meterRegistry)
                .increment();
        log.error("Error recorded: type={}, message={}", errorType, message);
    }
}
