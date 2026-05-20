package com.example.couponpublish.coupon.event;

import com.example.couponpublish.coupon.config.KafkaTopicProperties;
import com.example.couponpublish.outbox.OutboxEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "coupon.kafka", name = "enabled", havingValue = "true")
public class OutboxCouponEventPublisher implements CouponEventPublisher {

    private static final String AGGREGATE_TYPE = "COUPON";

    private final OutboxEventPublisher outboxEventPublisher;
    private final KafkaTopicProperties topicProperties;

    public OutboxCouponEventPublisher(
        OutboxEventPublisher outboxEventPublisher,
        KafkaTopicProperties topicProperties
    ) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publishIssued(CouponIssuedEvent event) {
        outboxEventPublisher.publish(
            AGGREGATE_TYPE,
            String.valueOf(event.couponId()),
            "COUPON_ISSUED",
            topicProperties.couponIssued(),
            event.userId(),
            event
        );
    }
}
