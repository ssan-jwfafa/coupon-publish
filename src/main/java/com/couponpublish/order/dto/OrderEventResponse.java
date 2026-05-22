package com.couponpublish.order.dto;

import com.couponpublish.order.entity.OrderEvent;
import com.couponpublish.order.entity.OrderEventType;
import java.time.LocalDateTime;

public record OrderEventResponse(
    Long eventId,
    String orderId,
    OrderEventType type,
    String message,
    LocalDateTime occurredAt
) {

    public static OrderEventResponse from(OrderEvent event) {
        return new OrderEventResponse(
            event.getEventId(),
            event.getOrderId(),
            event.getType(),
            event.getMessage(),
            event.getOccurredAt()
        );
    }
}
