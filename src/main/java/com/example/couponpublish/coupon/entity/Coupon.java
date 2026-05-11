package com.example.couponpublish.coupon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "coupon")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "max_count", nullable = false)
    private int maxCount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    private Coupon(String name, int maxCount, LocalDateTime startAt, LocalDateTime endAt) {
        this.name = name;
        this.maxCount = maxCount;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Coupon create(String name, int maxCount, LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("쿠폰 시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        return new Coupon(name, maxCount, startAt, endAt);
    }

    public boolean isBeforeIssuePeriod(LocalDateTime now) {
        return now.isBefore(startAt);
    }

    public boolean isAfterIssuePeriod(LocalDateTime now) {
        return now.isAfter(endAt);
    }
}
