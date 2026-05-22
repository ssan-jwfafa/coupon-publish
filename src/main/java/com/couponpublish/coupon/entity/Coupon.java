package com.couponpublish.coupon.entity;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class Coupon {

    private Long id;

    private String name;

    private int maxCount;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Coupon(Long id, String name, int maxCount, LocalDateTime startAt, LocalDateTime endAt) {
        this.id = id;
        this.name = name;
        this.maxCount = maxCount;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static Coupon create(String name, int maxCount, LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("쿠폰 시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        return new Coupon(null, name, maxCount, startAt, endAt);
    }

    public Coupon withId(Long id) {
        return new Coupon(id, name, maxCount, startAt, endAt);
    }

    public boolean isBeforeIssuePeriod(LocalDateTime now) {
        return now.isBefore(startAt);
    }

    public boolean isAfterIssuePeriod(LocalDateTime now) {
        return now.isAfter(endAt);
    }
}
