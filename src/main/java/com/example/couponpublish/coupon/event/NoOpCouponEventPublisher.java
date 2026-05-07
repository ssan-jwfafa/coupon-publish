package com.example.couponpublish.coupon.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "coupon.kafka", name = "enabled", havingValue = "false")
public class NoOpCouponEventPublisher implements CouponEventPublisher {

    @Override
    public void publishIssued(CouponIssuedEvent event) {
    }
}
