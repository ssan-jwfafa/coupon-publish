package com.example.couponpublish.coupon.dto;

import com.example.couponpublish.coupon.entity.Coupon;
import java.time.LocalDateTime;

public record CouponResponse(
    Long couponId,
    String name,
    int maxCount,
    LocalDateTime startAt,
    LocalDateTime endAt
) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
            coupon.getId(),
            coupon.getName(),
            coupon.getMaxCount(),
            coupon.getStartAt(),
            coupon.getEndAt()
        );
    }
}
