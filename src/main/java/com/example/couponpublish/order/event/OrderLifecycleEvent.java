package com.example.couponpublish.order.event;

import com.example.couponpublish.order.entity.Order;
import com.example.couponpublish.order.entity.OrderEvent;
import com.example.couponpublish.order.entity.OrderEventType;
import com.example.couponpublish.order.entity.OrderStatus;
import com.example.couponpublish.order.entity.PaymentMethod;
import java.time.LocalDateTime;

public record OrderLifecycleEvent(
    Long eventId,
    String orderId,
    OrderEventType type,
    String message,
    OrderStatus previousStatus,
    OrderStatus status,
    PaymentMethod paymentMethod,
    long amount,
    LocalDateTime occurredAt
) {

    public static OrderLifecycleEvent created(Order order, OrderEvent event) {
        return new OrderLifecycleEvent(
            event.getEventId(),
            order.getOrderId(),
            event.getType(),
            event.getMessage(),
            null,
            order.getStatus(),
            order.getPaymentMethod(),
            order.getAmount(),
            event.getOccurredAt()
        );
    }

    public static OrderLifecycleEvent statusChanged(Order order, OrderEvent event, OrderStatus previousStatus) {
        return new OrderLifecycleEvent(
            event.getEventId(),
            order.getOrderId(),
            event.getType(),
            event.getMessage(),
            previousStatus,
            order.getStatus(),
            order.getPaymentMethod(),
            order.getAmount(),
            event.getOccurredAt()
        );
    }
}
