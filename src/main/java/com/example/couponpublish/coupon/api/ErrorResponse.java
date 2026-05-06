package com.example.couponpublish.coupon.api;

import java.time.LocalDateTime;

public record ErrorResponse(
    String message,
    LocalDateTime timestamp
) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, LocalDateTime.now());
    }
}
