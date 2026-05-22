package com.couponpublish.coupon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon.kafka.topics")
public record KafkaTopicProperties(
    String couponIssued
) {
}
