package com.example.couponpublish.order.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "order.kafka", name = "enabled", havingValue = "false")
public class NoOpOrderEventPublisher implements OrderEventPublisher {

    @Override
    public void publish(OrderLifecycleEvent event) {
    }
}
