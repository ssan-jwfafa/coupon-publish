package com.example.couponpublish.order.flink;

import com.example.couponpublish.order.entity.OrderEventType;
import com.example.couponpublish.order.entity.OrderStatus;
import com.example.couponpublish.order.entity.PaymentMethod;

public class OrderLifecycleStatisticsEvent {

    private Long eventId;
    private String orderId;
    private OrderEventType type;
    private String message;
    private OrderStatus previousStatus;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private long amount;
    private String occurredAt;

    public OrderLifecycleStatisticsEvent() {
    }

    public OrderLifecycleStatisticsEvent(
        Long eventId,
        String orderId,
        OrderEventType type,
        String message,
        OrderStatus previousStatus,
        OrderStatus status,
        PaymentMethod paymentMethod,
        long amount,
        String occurredAt
    ) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.type = type;
        this.message = message;
        this.previousStatus = previousStatus;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderEventType getType() {
        return type;
    }

    public void setType(OrderEventType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(OrderStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }
}
