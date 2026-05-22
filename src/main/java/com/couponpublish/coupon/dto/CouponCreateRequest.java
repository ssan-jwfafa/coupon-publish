package com.couponpublish.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record CouponCreateRequest(
    @NotBlank(message = "쿠폰 이름은 필수입니다.")
    String name,

    @Positive(message = "최대 발급 수량은 1 이상이어야 합니다.")
    int maxCount,

    @NotNull(message = "발급 시작 시간은 필수입니다.")
    LocalDateTime startAt,

    @NotNull(message = "발급 종료 시간은 필수입니다.")
    LocalDateTime endAt
) {
}
