package com.example.couponpublish.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.couponpublish.coupon.entity.Coupon;
import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
import com.example.couponpublish.coupon.event.CouponEventPublisher;
import com.example.couponpublish.coupon.exception.CouponException;
import com.example.couponpublish.coupon.repository.CouponIssueRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository;
import com.example.couponpublish.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CouponServiceFeatureTest {

    @Mock
    CouponRepository couponRepository;

    @Mock
    CouponIssueRepository couponIssueRepository;

    @Mock
    CouponRedisRepository couponRedisRepository;

    @Mock
    CouponEventPublisher couponEventPublisher;

    @InjectMocks
    CouponService couponService;

    @Test
    void blockIssueBeforeCouponStartAt() {
        Coupon coupon = coupon(
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().plusHours(1)
        );
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.issue(1L, "user-1"))
            .isInstanceOf(CouponException.class)
            .hasMessage("쿠폰 발급 시작 전입니다.");

        verify(couponRedisRepository, never()).issue(any(), any(), eq(100));
    }

    @Test
    void getIssuesByStatusWithPaging() {
        Coupon coupon = coupon(
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusHours(1)
        );
        CouponIssue issue = CouponIssue.issue(coupon, "user-1");
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponIssueRepository.findAllByCoupon_IdAndStatus(eq(1L), eq(CouponStatus.ISSUED), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(issue), PageRequest.of(0, 20), 1));

        var response = couponService.getIssues(1L, CouponStatus.ISSUED, 0, 20);

        assertThat(response.contents()).hasSize(1);
        assertThat(response.contents().getFirst().userId()).isEqualTo("user-1");
        assertThat(response.totalElements()).isOne();
    }

    private static Coupon coupon(LocalDateTime startAt, LocalDateTime endAt) {
        return Coupon.create("test coupon", 100, startAt, endAt).withId(1L);
    }
}
