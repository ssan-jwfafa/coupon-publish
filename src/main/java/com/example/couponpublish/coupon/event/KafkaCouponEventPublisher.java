package com.example.couponpublish.coupon.event;

import com.example.couponpublish.coupon.config.KafkaTopicProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@ConditionalOnProperty(prefix = "coupon.kafka", name = "enabled", havingValue = "true")
public class KafkaCouponEventPublisher implements CouponEventPublisher {

    private final KafkaTemplate<String, CouponIssuedEvent> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public KafkaCouponEventPublisher(
        KafkaTemplate<String, CouponIssuedEvent> kafkaTemplate,
        KafkaTopicProperties topicProperties
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publishIssued(CouponIssuedEvent event) {
        Runnable publish = () -> kafkaTemplate.send(topicProperties.couponIssued(), event.userId(), event);
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
