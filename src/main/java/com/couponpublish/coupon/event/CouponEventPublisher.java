package com.couponpublish.coupon.event;

public interface CouponEventPublisher {

    void publishIssued(CouponIssuedEvent event);
}
