package com.couponpublish.order.dto;

import com.couponpublish.order.entity.Order;
import com.couponpublish.order.entity.OrderRisk;
import com.couponpublish.order.entity.OrderStatus;
import com.couponpublish.order.entity.PaymentMethod;
import java.time.LocalDateTime;

public record OrderResponse(
    String orderId,
    String customerName,
    String productName,
    OrderStatus status,
    PaymentMethod paymentMethod,
    long amount,
    String couponCode,
    String address,
    OrderRisk risk,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getOrderId(),
            order.getCustomerName(),
            order.getProductName(),
            order.getStatus(),
            order.getPaymentMethod(),
            order.getAmount(),
            order.getCouponCode(),
            order.getAddress(),
            order.getRisk(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
