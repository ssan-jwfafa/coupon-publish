package com.couponpublish.order.entity;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class OrderEvent {

    private Long eventId;
    private String orderId;
    private OrderEventType type;
    private String message;
    private LocalDateTime occurredAt;

    protected OrderEvent() {
    }

    private OrderEvent(Long eventId, String orderId, OrderEventType type, String message, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.type = type;
        this.message = message;
        this.occurredAt = occurredAt;
    }

    public static OrderEvent create(String orderId, OrderEventType type, String message) {
        return new OrderEvent(null, orderId, type, message, LocalDateTime.now());
    }

    public OrderEvent withId(Long eventId) {
        return new OrderEvent(eventId, orderId, type, message, occurredAt);
    }
}
