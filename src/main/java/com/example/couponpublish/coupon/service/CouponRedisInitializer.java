package com.example.couponpublish.coupon.service;

import com.example.couponpublish.coupon.entity.Coupon;
import com.example.couponpublish.coupon.entity.CouponStatus;
import com.example.couponpublish.coupon.repository.CouponIssueRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository;
import com.example.couponpublish.coupon.repository.CouponRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CouponRedisInitializer implements ApplicationRunner {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponRedisRepository couponRedisRepository;

    public CouponRedisInitializer(
        CouponRepository couponRepository,
        CouponIssueRepository couponIssueRepository,
        CouponRedisRepository couponRedisRepository
    ) {
        this.couponRepository = couponRepository;
        this.couponIssueRepository = couponIssueRepository;
        this.couponRedisRepository = couponRedisRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Coupon coupon : couponRepository.findAll()) {
            var activeUserIds = couponIssueRepository.findAllByCouponIdAndStatus(coupon.getId(), CouponStatus.ISSUED)
                .stream()
                .map(issue -> issue.getUserId())
                .collect(java.util.stream.Collectors.toSet());

            couponRedisRepository.resetFromActiveUsers(coupon.getId(), coupon.getMaxCount(), activeUserIds);
        }
    }
}
