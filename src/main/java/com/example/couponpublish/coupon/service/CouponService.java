package com.example.couponpublish.coupon.service;

import com.example.couponpublish.coupon.dto.CouponIssueResponse;
import com.example.couponpublish.coupon.dto.CouponRemainingResponse;
import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
import com.example.couponpublish.coupon.exception.CouponException;
import com.example.couponpublish.coupon.repository.CouponIssueRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository;
import com.example.couponpublish.coupon.repository.CouponRedisRepository.IssueResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponRedisRepository couponRedisRepository;

    public CouponService(CouponIssueRepository couponIssueRepository, CouponRedisRepository couponRedisRepository) {
        this.couponIssueRepository = couponIssueRepository;
        this.couponRedisRepository = couponRedisRepository;
    }

    @Transactional
    public CouponIssueResponse issue(String userId) {
        IssueResult result = couponRedisRepository.issue(userId);
        if (result == IssueResult.DUPLICATED) {
            throw new CouponException(HttpStatus.CONFLICT, "이미 발급된 사용자입니다.");
        }
        if (result == IssueResult.SOLD_OUT) {
            throw new CouponException(HttpStatus.CONFLICT, "쿠폰이 모두 소진되었습니다.");
        }

        try {
            CouponIssue couponIssue = couponIssueRepository.findByUserIdForUpdate(userId)
                .map(existing -> {
                    if (existing.getStatus() == CouponStatus.ISSUED) {
                        throw new CouponException(HttpStatus.CONFLICT, "이미 발급된 사용자입니다.");
                    }
                    existing.reissue();
                    return existing;
                })
                .orElseGet(() -> CouponIssue.issue(userId));

            CouponIssue saved = couponIssueRepository.save(couponIssue);
            return CouponIssueResponse.from(saved);
        } catch (CouponException | DataIntegrityViolationException ex) {
            couponRedisRepository.rollbackIssue(userId);
            if (ex instanceof CouponException couponException) {
                throw couponException;
            }
            throw new CouponException(HttpStatus.CONFLICT, "이미 발급된 사용자입니다.");
        }
    }

    @Transactional(readOnly = true)
    public CouponIssueResponse getIssue(String userId) {
        CouponIssue couponIssue = couponIssueRepository.findByUserId(userId)
            .orElseThrow(() -> new CouponException(HttpStatus.NOT_FOUND, "쿠폰 발급 내역이 없습니다."));
        return CouponIssueResponse.from(couponIssue);
    }

    @Transactional(readOnly = true)
    public CouponRemainingResponse getRemainingCount() {
        return new CouponRemainingResponse(couponRedisRepository.getRemainingCount());
    }

    @Transactional
    public CouponIssueResponse cancel(String userId) {
        CouponIssue couponIssue = couponIssueRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new CouponException(HttpStatus.NOT_FOUND, "쿠폰 발급 내역이 없습니다."));

        if (couponIssue.getStatus() == CouponStatus.CANCELED) {
            throw new CouponException(HttpStatus.CONFLICT, "이미 취소된 쿠폰입니다.");
        }

        couponIssue.cancel();
        couponRedisRepository.cancel(userId);
        return CouponIssueResponse.from(couponIssue);
    }
}
