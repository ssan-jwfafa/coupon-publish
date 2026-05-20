package com.example.couponpublish.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order.kafka.topics")
public record OrderKafkaTopicProperties(
    String events
) {
}
