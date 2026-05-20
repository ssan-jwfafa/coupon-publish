package com.example.couponpublish.order.event;

import com.example.couponpublish.order.config.OrderKafkaTopicProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@ConditionalOnProperty(prefix = "order.kafka", name = "enabled", havingValue = "true")
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, OrderLifecycleEvent> kafkaTemplate;
    private final OrderKafkaTopicProperties topicProperties;

    public KafkaOrderEventPublisher(
        KafkaTemplate<String, OrderLifecycleEvent> kafkaTemplate,
        OrderKafkaTopicProperties topicProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publish(OrderLifecycleEvent event) {
        Runnable publish = () -> kafkaTemplate.send(topicProperties.events(), event.orderId(), event);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }
}
