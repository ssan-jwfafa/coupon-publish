package com.couponpublish.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "outbox.relay", name = "enabled", havingValue = "true")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxRelayProperties properties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxRelay(
        OutboxEventRepository outboxEventRepository,
        OutboxRelayProperties properties,
        KafkaTemplate<String, Object> kafkaTemplate,
        ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.properties = properties;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay:1000}")
    public void relay() {
        for (OutboxEvent event : outboxEventRepository.findPublishable(properties.batchSize(), properties.maxAttempts())) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.payload());
            kafkaTemplate.send(event.topic(), event.eventKey(), payload).get(3, TimeUnit.SECONDS);
            outboxEventRepository.markPublished(event.id());
        } catch (Exception ex) {
            log.warn("outbox event publish failed. id={}, topic={}", event.id(), event.topic(), ex);
            outboxEventRepository.markFailed(event.id(), ex.getMessage());
        }
    }
}
