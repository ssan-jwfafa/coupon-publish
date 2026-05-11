package com.example.couponpublish.coupon.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class CouponConfig {
}
