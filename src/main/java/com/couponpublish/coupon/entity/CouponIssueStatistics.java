package com.couponpublish.coupon.entity;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class CouponIssueStatistics {

    private Long couponId;
    private long issuedCount;
    private LocalDateTime lastIssuedAt;
    private LocalDateTime updatedAt;

    public CouponIssueStatistics(Long couponId, long issuedCount, LocalDateTime lastIssuedAt, LocalDateTime updatedAt) {
        this.couponId = couponId;
        this.issuedCount = issuedCount;
        this.lastIssuedAt = lastIssuedAt;
        this.updatedAt = updatedAt;
    }

    public static CouponIssueStatistics firstIssued(Long couponId, LocalDateTime issuedAt) {
        return new CouponIssueStatistics(couponId, 1, issuedAt, LocalDateTime.now());
    }

    public void increaseIssuedCount(LocalDateTime issuedAt) {
        this.issuedCount++;
        if (issuedAt.isAfter(lastIssuedAt)) {
            this.lastIssuedAt = issuedAt;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
