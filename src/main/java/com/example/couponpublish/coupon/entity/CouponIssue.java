package com.example.couponpublish.coupon.entity;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class CouponIssue {

    private Long id;
    private Long couponId;
    private String userId;
    private CouponStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime canceledAt;

    protected CouponIssue() {
    }

    private CouponIssue(Long id, Long couponId, String userId, CouponStatus status, LocalDateTime issuedAt, LocalDateTime canceledAt) {
        this.id = id;
        this.couponId = couponId;
        this.userId = userId;
        this.status = status;
        this.issuedAt = issuedAt;
        this.canceledAt = canceledAt;
    }

    public static CouponIssue issue(Coupon coupon, String userId) {
        return new CouponIssue(null, coupon.getId(), userId, CouponStatus.ISSUED, LocalDateTime.now(), null);
    }

    public CouponIssue withId(Long id) {
        return new CouponIssue(id, couponId, userId, status, issuedAt, canceledAt);
    }

    public void reissue() {
        this.status = CouponStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
        this.canceledAt = null;
    }

    public void cancel() {
        this.status = CouponStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }
}
