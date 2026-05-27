package com.couponpublish.outbox;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusInAndAttemptsLessThanOrderByIdAsc(
        Collection<OutboxEventStatus> statuses,
        int attempts,
        Pageable pageable
    );
}
