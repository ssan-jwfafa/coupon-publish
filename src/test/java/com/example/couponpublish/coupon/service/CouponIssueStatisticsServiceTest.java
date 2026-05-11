package com.example.couponpublish.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.couponpublish.coupon.entity.CouponIssueStatistics;
import com.example.couponpublish.coupon.entity.CouponStatus;
import com.example.couponpublish.coupon.event.CouponIssuedEvent;
import com.example.couponpublish.coupon.repository.CouponIssueStatisticsRepository;
import com.example.couponpublish.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void createStatisticsWhenFirstIssuedEventIsConsumed() {
        LocalDateTime issuedAt = LocalDateTime.now();
        CouponIssuedEvent event = new CouponIssuedEvent(1L, 10L, "user-1", CouponStatus.ISSUED, issuedAt);
        when(statisticsRepository.findByCouponIdForUpdate(10L)).thenReturn(Optional.empty());

        statisticsService.aggregateIssuedEvent(event);

        ArgumentCaptor<CouponIssueStatistics> captor = ArgumentCaptor.forClass(CouponIssueStatistics.class);
        verify(statisticsRepository).save(captor.capture());
        assertThat(captor.getValue().getCouponId()).isEqualTo(10L);
        assertThat(captor.getValue().getIssuedCount()).isOne();
        assertThat(captor.getValue().getLastIssuedAt()).isEqualTo(issuedAt);
    }

    @Test
    void increaseStatisticsWhenIssuedEventIsConsumedAgain() {
        LocalDateTime firstIssuedAt = LocalDateTime.now().minusMinutes(1);
        LocalDateTime secondIssuedAt = LocalDateTime.now();
        CouponIssueStatistics statistics = CouponIssueStatistics.firstIssued(10L, firstIssuedAt);
        CouponIssuedEvent event = new CouponIssuedEvent(2L, 10L, "user-2", CouponStatus.ISSUED, secondIssuedAt);
        when(statisticsRepository.findByCouponIdForUpdate(10L)).thenReturn(Optional.of(statistics));

        statisticsService.aggregateIssuedEvent(event);

        assertThat(statistics.getIssuedCount()).isEqualTo(2);
        assertThat(statistics.getLastIssuedAt()).isEqualTo(secondIssuedAt);
        verify(statisticsRepository).save(statistics);
    }
}
