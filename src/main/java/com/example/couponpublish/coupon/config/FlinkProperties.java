package com.example.couponpublish.coupon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon.flink")
public record FlinkProperties(
    boolean enabled,
    String consumerGroupId
) {
}
