package com.example.couponpublish.coupon.event;

public interface CouponEventPublisher {

    void publishIssued(CouponIssuedEvent event);
}
