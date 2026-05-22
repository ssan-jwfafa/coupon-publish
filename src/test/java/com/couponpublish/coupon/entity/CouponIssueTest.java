package com.couponpublish.coupon.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CouponIssueTest {

    @Test
    void issueCoupon() {
        Coupon coupon = activeCoupon();
        CouponIssue couponIssue = CouponIssue.issue(coupon, "user-1");

        assertThat(couponIssue.getCouponId()).isEqualTo(coupon.getId());
        assertThat(couponIssue.getUserId()).isEqualTo("user-1");
        assertThat(couponIssue.getStatus()).isEqualTo(CouponStatus.ISSUED);
        assertThat(couponIssue.getIssuedAt()).isNotNull();
        assertThat(couponIssue.getCanceledAt()).isNull();
    }

    @Test
    void cancelCoupon() {
        CouponIssue couponIssue = CouponIssue.issue(activeCoupon(), "user-1");

        couponIssue.cancel();

        assertThat(couponIssue.getStatus()).isEqualTo(CouponStatus.CANCELED);
        assertThat(couponIssue.getCanceledAt()).isNotNull();
    }

    @Test
    void reissueCanceledCoupon() {
        CouponIssue couponIssue = CouponIssue.issue(activeCoupon(), "user-1");
        couponIssue.cancel();

        couponIssue.reissue();

        assertThat(couponIssue.getStatus()).isEqualTo(CouponStatus.ISSUED);
        assertThat(couponIssue.getCanceledAt()).isNull();
    }

    private static Coupon activeCoupon() {
        return Coupon.create(
            "test coupon",
            100,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusHours(1)
        ).withId(1L);
    }
}
