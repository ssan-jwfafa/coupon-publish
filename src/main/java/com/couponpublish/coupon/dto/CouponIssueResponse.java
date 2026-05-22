package com.couponpublish.coupon.dto;

import com.couponpublish.coupon.entity.CouponIssue;
import com.couponpublish.coupon.entity.CouponStatus;
import java.time.LocalDateTime;

public record CouponIssueResponse(
    Long couponIssueId,
    Long couponId,
    String userId,
    CouponStatus status,
    LocalDateTime issuedAt,
    LocalDateTime canceledAt
) {

    public static CouponIssueResponse from(CouponIssue couponIssue) {
        return new CouponIssueResponse(
            couponIssue.getId(),
            couponIssue.getCouponId(),
            couponIssue.getUserId(),
            couponIssue.getStatus(),
            couponIssue.getIssuedAt(),
            couponIssue.getCanceledAt()
        );
    }
}
