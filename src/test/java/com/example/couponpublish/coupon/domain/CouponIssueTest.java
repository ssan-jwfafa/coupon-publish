package com.example.couponpublish.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CouponIssueTest {

    @Test
    void issueCoupon() {
        CouponIssue couponIssue = CouponIssue.issue("user-1");

        assertThat(couponIssue.getUserId()).isEqualTo("user-1");
        assertThat(couponIssue.getStatus()).isEqualTo(CouponStatus.ISSUED);
        assertThat(couponIssue.getIssuedAt()).isNotNull();
        assertThat(couponIssue.getCanceledAt()).isNull();
    }

    @Test
    void cancelCoupon() {
        CouponIssue couponIssue = CouponIssue.issue("user-1");

        couponIssue.cancel();

        assertThat(couponIssue.getStatus()).isEqualTo(CouponStatus.CANCELED);
        assertThat(couponIssue.getCanceledAt()).isNotNull();
    }

    @Test
    void reissueCanceledCoupon() {
        CouponIssue couponIssue = CouponIssue.issue("user-1");
        couponIssue.cancel();

        couponIssue.reissue();

        assertThat(couponIssue.getStatus()).isEqualTo(CouponStatus.ISSUED);
        assertThat(couponIssue.getCanceledAt()).isNull();
    }
}
