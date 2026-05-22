package com.couponpublish.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.couponpublish.coupon.dto.CouponIssueStatisticsResponse;
import com.couponpublish.coupon.entity.CouponIssueStatistics;
import com.couponpublish.coupon.repository.CouponIssueStatisticsRepository;
import com.couponpublish.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponIssueStatisticsServiceTest {

    @Mock
    CouponRepository couponRepository;

    @Mock
    CouponIssueStatisticsRepository statisticsRepository;

    @InjectMocks
    CouponIssueStatisticsService statisticsService;

    @Test
    void getStatisticsFromRedisRepository() {
        LocalDateTime lastIssuedAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        CouponIssueStatistics statistics = new CouponIssueStatistics(10L, 2, lastIssuedAt, updatedAt);
        when(couponRepository.existsById(10L)).thenReturn(true);
        when(statisticsRepository.findByCouponId(10L)).thenReturn(Optional.of(statistics));

        CouponIssueStatisticsResponse response = statisticsService.getStatistics(10L);

        assertThat(response.couponId()).isEqualTo(10L);
        assertThat(response.issuedCount()).isEqualTo(2);
        assertThat(response.lastIssuedAt()).isEqualTo(lastIssuedAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }
}
