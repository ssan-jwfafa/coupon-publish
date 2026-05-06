package com.example.couponpublish.coupon.dto;

import jakarta.validation.constraints.NotBlank;

public record CouponIssueRequest(
    @NotBlank(message = "userId는 필수입니다.")
    String userId
) {
}
