package com.example.couponpublish.coupon.exception;

import org.springframework.http.HttpStatus;

public class CouponException extends RuntimeException {

    private final HttpStatus status;

    public CouponException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
