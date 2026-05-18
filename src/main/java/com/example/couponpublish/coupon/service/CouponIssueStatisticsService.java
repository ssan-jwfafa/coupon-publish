package com.example.couponpublish.coupon.service;

import com.example.couponpublish.coupon.dto.CouponIssueStatisticsResponse;
import com.example.couponpublish.coupon.exception.CouponException;
import com.example.couponpublish.coupon.repository.CouponIssueStatisticsRepository;
import com.example.couponpublish.coupon.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CouponIssueStatisticsService {

    private final CouponRepository couponRepository;
    private final CouponIssueStatisticsRepository statisticsRepository;

    public CouponIssueStatisticsService(
        CouponRepository couponRepository,
        CouponIssueStatisticsRepository statisticsRepository
    ) {
        this.couponRepository = couponRepository;
        this.statisticsRepository = statisticsRepository;
    }

    public CouponIssueStatisticsResponse getStatistics(Long couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new CouponException(HttpStatus.NOT_FOUND, "쿠폰이 없습니다.");
        }

        return statisticsRepository.findByCouponId(couponId)
            .map(CouponIssueStatisticsResponse::from)
            .orElseGet(() -> CouponIssueStatisticsResponse.empty(couponId));
    }
}
