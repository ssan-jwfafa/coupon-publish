package com.example.couponpublish.coupon.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CouponProperties.class, KafkaTopicProperties.class})
public class CouponConfig {
}
