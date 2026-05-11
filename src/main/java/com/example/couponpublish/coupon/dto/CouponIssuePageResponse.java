package com.example.couponpublish.coupon.dto;

import com.example.couponpublish.coupon.entity.CouponIssue;
import java.util.List;
import org.springframework.data.domain.Page;

public record CouponIssuePageResponse(
    List<CouponIssueResponse> contents,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    public static CouponIssuePageResponse from(Page<CouponIssue> couponIssues) {
        return new CouponIssuePageResponse(
            couponIssues.stream()
                .map(CouponIssueResponse::from)
                .toList(),
            couponIssues.getNumber(),
            couponIssues.getSize(),
            couponIssues.getTotalElements(),
            couponIssues.getTotalPages()
        );
    }
}
