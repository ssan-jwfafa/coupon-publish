package com.example.couponpublish.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "coupon_issue",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_coupon_issue_user_id", columnNames = "user_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    private CouponIssue(String userId) {
        this.userId = userId;
        this.status = CouponStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
    }

    public static CouponIssue issue(String userId) {
        return new CouponIssue(userId);
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
