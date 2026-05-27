package com.couponpublish.outbox;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OutboxEventRepository {

    private final OutboxEventJpaRepository jpaRepository;

    public OutboxEventRepository(OutboxEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Transactional
    public void save(
        String aggregateType,
        String aggregateId,
        String eventType,
        String topic,
        String eventKey,
        String payload
    ) {
        jpaRepository.save(OutboxEvent.pending(
            aggregateType,
            aggregateId,
            eventType,
            topic,
            eventKey,
            payload
        ));
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> findPublishable(int limit, int maxAttempts) {
        return jpaRepository.findByStatusInAndAttemptsLessThanOrderByIdAsc(
            List.of(OutboxEventStatus.PENDING, OutboxEventStatus.FAILED),
            maxAttempts,
            PageRequest.of(0, limit)
        );
    }

    @Transactional
    public void markPublished(Long id) {
        jpaRepository.findById(id).ifPresent(OutboxEvent::markPublished);
    }

    @Transactional
    public void markFailed(Long id, String errorMessage) {
        jpaRepository.findById(id)
            .ifPresent(event -> event.markFailed(truncate(errorMessage)));
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 1_000) {
            return value;
        }
        return value.substring(0, 1_000);
    }
}
