package com.example.couponpublish.coupon.flink;

public class CouponIssuedStatisticsEvent {

    private Long couponIssueId;
    private Long couponId;
    private String userId;
    private String issuedAt;

    public CouponIssuedStatisticsEvent() {
    }

    public CouponIssuedStatisticsEvent(Long couponIssueId, Long couponId, String userId, String issuedAt) {
        this.couponIssueId = couponIssueId;
        this.couponId = couponId;
        this.userId = userId;
        this.issuedAt = issuedAt;
    }

    public Long getCouponIssueId() {
        return couponIssueId;
    }

    public void setCouponIssueId(Long couponIssueId) {
        this.couponIssueId = couponIssueId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(String issuedAt) {
        this.issuedAt = issuedAt;
    }
}
