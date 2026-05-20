package com.example.couponpublish.order.event;

import com.example.couponpublish.order.config.OrderKafkaTopicProperties;
import com.example.couponpublish.outbox.OutboxEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "order.kafka", name = "enabled", havingValue = "true")
public class OutboxOrderEventPublisher implements OrderEventPublisher {

    private static final String AGGREGATE_TYPE = "ORDER";

    private final OutboxEventPublisher outboxEventPublisher;
    private final OrderKafkaTopicProperties topicProperties;

    public OutboxOrderEventPublisher(
        OutboxEventPublisher outboxEventPublisher,
        OrderKafkaTopicProperties topicProperties
    ) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(OrderLifecycleEvent event) {
        outboxEventPublisher.publish(
            AGGREGATE_TYPE,
            event.orderId(),
            event.type().name(),
            topicProperties.events(),
            event.orderId(),
            event
        );
    }
}
