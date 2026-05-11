package com.example.couponpublish.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "coupon_issue_statistics",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_coupon_issue_statistics_coupon_id", columnNames = "coupon_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssueStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "issued_count", nullable = false)
    private long issuedCount;

    @Column(name = "last_issued_at", nullable = false)
    private LocalDateTime lastIssuedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private CouponIssueStatistics(Long couponId, LocalDateTime issuedAt) {
        this.couponId = couponId;
        this.issuedCount = 1;
        this.lastIssuedAt = issuedAt;
        this.updatedAt = LocalDateTime.now();
    }

    public static CouponIssueStatistics firstIssued(Long couponId, LocalDateTime issuedAt) {
        return new CouponIssueStatistics(couponId, issuedAt);
    }

    public void increaseIssuedCount(LocalDateTime issuedAt) {
        this.issuedCount++;
        if (issuedAt.isAfter(lastIssuedAt)) {
            this.lastIssuedAt = issuedAt;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
