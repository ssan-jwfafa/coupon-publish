package com.example.couponpublish.coupon.service;

import com.example.couponpublish.coupon.dto.CouponIssueStatisticsResponse;
import com.example.couponpublish.coupon.entity.CouponIssueStatistics;
import com.example.couponpublish.coupon.event.CouponIssuedEvent;
import com.example.couponpublish.coupon.exception.CouponException;
import com.example.couponpublish.coupon.repository.CouponIssueStatisticsRepository;
import com.example.couponpublish.coupon.repository.CouponRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public void aggregateIssuedEvent(CouponIssuedEvent event) {
        CouponIssueStatistics statistics = statisticsRepository.findByCouponIdForUpdate(event.couponId())
            .map(existing -> {
                existing.increaseIssuedCount(event.issuedAt());
                return existing;
            })
            .orElseGet(() -> CouponIssueStatistics.firstIssued(event.couponId(), event.issuedAt()));

        statisticsRepository.save(statistics);
    }

    @Transactional(readOnly = true)
    public CouponIssueStatisticsResponse getStatistics(Long couponId) {
        if (!couponRepository.existsById(couponId)) {
            throw new CouponException(HttpStatus.NOT_FOUND, "쿠폰이 없습니다.");
        }

        return statisticsRepository.findByCouponId(couponId)
            .map(CouponIssueStatisticsResponse::from)
            .orElseGet(() -> CouponIssueStatisticsResponse.empty(couponId));
    }
}
