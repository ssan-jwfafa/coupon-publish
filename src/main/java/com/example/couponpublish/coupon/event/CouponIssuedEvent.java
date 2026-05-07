package com.example.couponpublish.coupon.event;

import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
import java.time.LocalDateTime;

public record CouponIssuedEvent(
    Long couponIssueId,
    String userId,
    CouponStatus status,
    LocalDateTime issuedAt
) {

    public static CouponIssuedEvent from(CouponIssue couponIssue) {
        return new CouponIssuedEvent(
            couponIssue.getId(),
            couponIssue.getUserId(),
            couponIssue.getStatus(),
            couponIssue.getIssuedAt()
        );
    }
}
