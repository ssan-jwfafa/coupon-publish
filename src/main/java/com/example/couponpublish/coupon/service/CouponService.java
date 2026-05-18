package com.example.couponpublish.coupon.service;

import com.example.couponpublish.coupon.dto.CouponCreateRequest;
import com.example.couponpublish.coupon.dto.CouponIssuePageResponse;
import com.example.couponpublish.coupon.dto.CouponIssueResponse;
import com.example.couponpublish.coupon.dto.CouponRemainingResponse;
import com.example.couponpublish.coupon.dto.CouponResponse;
import com.example.couponpublish.coupon.entity.Coupon;
import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
import com.example.couponpublish.coupon.event.CouponEventPublisher;
import com.example.couponpublish.coupon.event.CouponIssuedEvent;
import com.example.couponpublish.coupon.exception.CouponException;
import com.example.couponpublish.coupon.repository.CouponIssueRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository.IssueResult;
import com.example.couponpublish.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponRedisRepository couponRedisRepository;
    private final CouponEventPublisher couponEventPublisher;

    public CouponService(
        CouponRepository couponRepository,
        CouponIssueRepository couponIssueRepository,
        CouponRedisRepository couponRedisRepository,
        CouponEventPublisher couponEventPublisher
    ) {
        this.couponRepository = couponRepository;
        this.couponIssueRepository = couponIssueRepository;
        this.couponRedisRepository = couponRedisRepository;
        this.couponEventPublisher = couponEventPublisher;
    }

    public CouponResponse createCoupon(CouponCreateRequest request) {
        Coupon coupon;
        try {
            coupon = Coupon.create(request.name(), request.maxCount(), request.startAt(), request.endAt());
        } catch (IllegalArgumentException ex) {
            throw new CouponException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        return CouponResponse.from(couponRepository.save(coupon));
    }

    public CouponResponse getCoupon(Long couponId) {
        return CouponResponse.from(getCouponOrThrow(couponId));
    }

    public CouponIssueResponse issue(Long couponId, String userId) {
        Coupon coupon = getCouponOrThrow(couponId);
        validateIssuePeriod(coupon);

        IssueResult result = couponRedisRepository.issue(coupon.getId(), userId, coupon.getMaxCount());
        if (result == IssueResult.DUPLICATED) {
            throw new CouponException(HttpStatus.CONFLICT, "이미 발급된 사용자입니다.");
        }
        if (result == IssueResult.SOLD_OUT) {
            throw new CouponException(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다.");
        }

        try {
            CouponIssue couponIssue = couponIssueRepository.findByCouponIdAndUserIdForUpdate(coupon.getId(), userId)
                .map(existing -> {
                    if (existing.getStatus() == CouponStatus.ISSUED) {
                        throw new CouponException(HttpStatus.CONFLICT, "이미 발급된 사용자입니다.");
                    }
                    existing.reissue();
                    return existing;
                })
                .orElseGet(() -> CouponIssue.issue(coupon, userId));

            CouponIssue saved = couponIssueRepository.save(couponIssue);
            couponEventPublisher.publishIssued(CouponIssuedEvent.from(saved));
            return CouponIssueResponse.from(saved);
        } catch (RuntimeException ex) {
            couponRedisRepository.rollbackIssue(coupon.getId(), userId);
            if (ex instanceof CouponException couponException) {
                throw couponException;
            }
            throw ex;
        }
    }

    public CouponIssueResponse getIssue(Long couponId, String userId) {
        CouponIssue couponIssue = couponIssueRepository.findByCoupon_IdAndUserId(couponId, userId)
            .orElseThrow(() -> new CouponException(HttpStatus.NOT_FOUND, "쿠폰 발급 내역이 없습니다."));
        return CouponIssueResponse.from(couponIssue);
    }

    public CouponIssuePageResponse getIssues(Long couponId, CouponStatus status, int page, int size) {
        getCouponOrThrow(couponId);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));
        if (status == null) {
            return CouponIssuePageResponse.from(couponIssueRepository.findAllByCoupon_Id(couponId, pageRequest));
        }
        return CouponIssuePageResponse.from(
            couponIssueRepository.findAllByCoupon_IdAndStatus(couponId, status, pageRequest)
        );
    }

    public CouponRemainingResponse getRemainingCount(Long couponId) {
        Coupon coupon = getCouponOrThrow(couponId);
        return new CouponRemainingResponse(couponRedisRepository.getRemainingCount(coupon.getId(), coupon.getMaxCount()));
    }

    public CouponIssueResponse cancel(Long couponId, String userId) {
        CouponIssue couponIssue = couponIssueRepository.findByCouponIdAndUserIdForUpdate(couponId, userId)
            .orElseThrow(() -> new CouponException(HttpStatus.NOT_FOUND, "쿠폰 발급 내역이 없습니다."));

        if (couponIssue.getStatus() == CouponStatus.CANCELED) {
            throw new CouponException(HttpStatus.CONFLICT, "이미 취소된 쿠폰입니다.");
        }

        couponIssue.cancel();
        couponIssueRepository.save(couponIssue);
        couponRedisRepository.cancel(couponId, userId);
        return CouponIssueResponse.from(couponIssue);
    }

    private Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
            .orElseThrow(() -> new CouponException(HttpStatus.NOT_FOUND, "쿠폰이 없습니다."));
    }

    private static void validateIssuePeriod(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.isBeforeIssuePeriod(now)) {
            throw new CouponException(HttpStatus.CONFLICT, "쿠폰 발급 시작 전입니다.");
        }
        if (coupon.isAfterIssuePeriod(now)) {
            throw new CouponException(HttpStatus.CONFLICT, "쿠폰 발급 기간이 종료되었습니다.");
        }
    }
}
