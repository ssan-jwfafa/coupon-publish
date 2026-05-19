package com.example.couponpublish.coupon.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.couponpublish.coupon.entity.Coupon;
import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.event.CouponEventPublisher;
import com.example.couponpublish.coupon.exception.CouponException;
import com.example.couponpublish.coupon.repository.CouponIssueRepository;
import com.example.couponpublish.coupon.repository.CouponIssueStatisticsRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository.IssueResult;
import com.example.couponpublish.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceFailureTest {

    @Mock
    CouponRepository couponRepository;

    @Mock
    CouponIssueRepository couponIssueRepository;

    @Mock
    CouponIssueStatisticsRepository couponIssueStatisticsRepository;

    @Mock
    CouponRedisRepository couponRedisRepository;

    @Mock
    CouponEventPublisher couponEventPublisher;

    @InjectMocks
    CouponService couponService;

    @Test
    void rollbackRedisWhenIssueHistorySaveFailsAfterRedisIssueSucceeds() {
        Coupon coupon = activeCoupon();
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRedisRepository.issue(1L, "user-1", 100)).thenReturn(IssueResult.SUCCESS);
        when(couponIssueRepository.findByCouponIdAndUserIdForUpdate(1L, "user-1")).thenReturn(Optional.empty());
        when(couponIssueRepository.save(any(CouponIssue.class)))
            .thenThrow(new IllegalStateException("redis write failed"));

        assertThatThrownBy(() -> couponService.issue(1L, "user-1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("redis write failed");

        verify(couponRedisRepository).rollbackIssue(1L, "user-1");
    }

    @Test
    void blockIssueWhenRedisIsUnavailable() {
        Coupon coupon = activeCoupon();
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRedisRepository.issue(1L, "user-1", 100))
            .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        assertThatThrownBy(() -> couponService.issue(1L, "user-1"))
            .isInstanceOf(RedisConnectionFailureException.class);

        verify(couponIssueRepository, never()).findByCouponIdAndUserIdForUpdate(anyLong(), anyString());
        verify(couponIssueRepository, never()).save(any(CouponIssue.class));
        verify(couponRedisRepository, never()).rollbackIssue(anyLong(), anyString());
    }

    private static Coupon activeCoupon() {
        return Coupon.create(
            "test coupon",
            100,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusHours(1)
        ).withId(1L);
    }
}
