package com.example.couponpublish.outbox;

import java.time.LocalDateTime;

public record OutboxEvent(
    Long id,
    String aggregateType,
    String aggregateId,
    String eventType,
    String topic,
    String eventKey,
    String payload,
    String status,
    int attempts,
    LocalDateTime createdAt,
    LocalDateTime publishedAt,
    String lastError
) {
}
