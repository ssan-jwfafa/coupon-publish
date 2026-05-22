package com.couponpublish.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void publish(
        String aggregateType,
        String aggregateId,
        String eventType,
        String topic,
        String eventKey,
        Object event
    ) {
        outboxEventRepository.save(
            aggregateType,
            aggregateId,
            eventType,
            topic,
            eventKey,
            payload(event)
        );
    }

    private String payload(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("outbox 이벤트를 직렬화할 수 없습니다.", ex);
        }
    }
}
