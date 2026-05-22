package com.couponpublish.coupon.dto;

import com.couponpublish.coupon.entity.CouponIssueStatistics;
import java.time.LocalDateTime;

public record CouponIssueStatisticsResponse(
    Long couponId,
    long issuedCount,
    LocalDateTime lastIssuedAt,
    LocalDateTime updatedAt
) {

    public static CouponIssueStatisticsResponse from(CouponIssueStatistics statistics) {
        return new CouponIssueStatisticsResponse(
            statistics.getCouponId(),
            statistics.getIssuedCount(),
            statistics.getLastIssuedAt(),
            statistics.getUpdatedAt()
        );
    }

    public static CouponIssueStatisticsResponse empty(Long couponId) {
        return new CouponIssueStatisticsResponse(couponId, 0, null, null);
    }
}
