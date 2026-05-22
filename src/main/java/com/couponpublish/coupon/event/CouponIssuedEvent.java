package com.couponpublish.coupon.event;

import com.couponpublish.coupon.entity.CouponIssue;
import com.couponpublish.coupon.entity.CouponStatus;
import java.time.LocalDateTime;

public record CouponIssuedEvent(
    Long couponIssueId,
    Long couponId,
    String userId,
    CouponStatus status,
    LocalDateTime issuedAt
) {

    public static CouponIssuedEvent from(CouponIssue couponIssue) {
        return new CouponIssuedEvent(
            couponIssue.getId(),
            couponIssue.getCouponId(),
            couponIssue.getUserId(),
            couponIssue.getStatus(),
            couponIssue.getIssuedAt()
        );
    }
}
