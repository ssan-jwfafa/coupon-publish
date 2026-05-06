package com.example.couponpublish.coupon.service;

import com.example.couponpublish.coupon.entity.CouponStatus;
import com.example.couponpublish.coupon.repository.CouponIssueRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CouponRedisInitializer implements ApplicationRunner {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponRedisRepository couponRedisRepository;

    public CouponRedisInitializer(
        CouponIssueRepository couponIssueRepository,
        CouponRedisRepository couponRedisRepository
    ) {
        this.couponIssueRepository = couponIssueRepository;
        this.couponRedisRepository = couponRedisRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        var activeUserIds = couponIssueRepository.findAllByStatus(CouponStatus.ISSUED)
            .stream()
            .map(issue -> issue.getUserId())
            .collect(Collectors.toSet());

        couponRedisRepository.resetFromActiveUsers(activeUserIds);
    }
}
