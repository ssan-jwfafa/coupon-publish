package com.example.couponpublish.coupon.dto;

import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
import java.time.LocalDateTime;

public record CouponIssueResponse(
    Long couponIssueId,
    String userId,
    CouponStatus status,
    LocalDateTime issuedAt,
    LocalDateTime canceledAt
) {

    public static CouponIssueResponse from(CouponIssue couponIssue) {
        return new CouponIssueResponse(
            couponIssue.getId(),
            couponIssue.getUserId(),
            couponIssue.getStatus(),
            couponIssue.getIssuedAt(),
            couponIssue.getCanceledAt()
        );
    }
}
