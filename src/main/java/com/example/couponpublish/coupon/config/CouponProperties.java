package com.example.couponpublish.coupon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon")
public record CouponProperties(int maxCount) {
}
