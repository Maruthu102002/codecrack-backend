package com.codecrack.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight metrics service for dev profile (no Redis/RabbitMQ needed)
 * Loads only when the full MetricsService can't be created
 */
@Service
@ConditionalOnMissingBean(MetricsService.class)
@Slf4j
public class DevMetricsService {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    public DevMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        log.info("DevMetricsService loaded (lightweight mode - no Redis/RabbitMQ)");
    }

    public void recordSubmission() {
        Counter.builder("submissions.total")
                .tag("type", "code")
                .register(meterRegistry)
                .increment();
    }

    public void recordExecutionTime(long milliseconds, String language) {
        Timer.builder("code.execution.time")
                .tag("language", language)
                .register(meterRegistry)
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }
}
