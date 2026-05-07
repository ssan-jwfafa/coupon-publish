package com.example.couponpublish.coupon.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.event.CouponEventPublisher;
import com.example.couponpublish.coupon.exception.CouponException;
import com.example.couponpublish.coupon.repository.CouponIssueRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository.IssueResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceFailureTest {

    @Mock
    CouponIssueRepository couponIssueRepository;

    @Mock
    CouponRedisRepository couponRedisRepository;

    @Mock
    CouponEventPublisher couponEventPublisher;

    @InjectMocks
    CouponService couponService;

    @Test
    void rollbackRedisWhenDatabaseSaveFailsAfterRedisIssueSucceeds() {
        when(couponRedisRepository.issue("user-1")).thenReturn(IssueResult.SUCCESS);
        when(couponIssueRepository.findByUserIdForUpdate("user-1")).thenReturn(Optional.empty());
        when(couponIssueRepository.save(any(CouponIssue.class)))
            .thenThrow(new DataIntegrityViolationException("duplicated user id"));

        assertThatThrownBy(() -> couponService.issue("user-1"))
            .isInstanceOf(CouponException.class)
            .hasMessage("이미 발급된 사용자입니다.");

        verify(couponRedisRepository).rollbackIssue("user-1");
    }

    @Test
    void blockIssueWhenRedisIsUnavailable() {
        when(couponRedisRepository.issue("user-1"))
            .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        assertThatThrownBy(() -> couponService.issue("user-1"))
            .isInstanceOf(RedisConnectionFailureException.class);

        verify(couponIssueRepository, never()).findByUserIdForUpdate(anyString());
        verify(couponIssueRepository, never()).save(any(CouponIssue.class));
        verify(couponRedisRepository, never()).rollbackIssue(anyString());
    }
}
