package com.example.couponpublish.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "outbox.relay")
public record OutboxRelayProperties(
    boolean enabled,
    int batchSize,
    int maxAttempts,
    long fixedDelay
) {
}
