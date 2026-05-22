package com.couponpublish.order.exception;

import org.springframework.http.HttpStatus;

public class OrderException extends RuntimeException {

    private final HttpStatus status;

    public OrderException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
