package com.example.couponpublish.order.entity;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class Order {

    private Long sequence;
    private String orderId;
    private String customerName;
    private String productName;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private long amount;
    private String couponCode;
    private String address;
    private OrderRisk risk;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Order() {
    }

    private Order(
        Long sequence,
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
        this.sequence = sequence;
        this.orderId = orderId;
        this.customerName = customerName;
        this.productName = productName;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.couponCode = couponCode;
        this.address = address;
        this.risk = risk;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(
        String customerName,
        String productName,
        PaymentMethod paymentMethod,
        long amount,
        String couponCode,
        String address
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new Order(
            null,
            null,
            customerName,
            productName,
            OrderStatus.PAYMENT_CONFIRMED,
            paymentMethod,
            amount,
            normalizeCouponCode(couponCode),
            address,
            calculateRisk(paymentMethod, amount),
            now,
            now
        );
    }

    public Order withIdentity(Long sequence) {
        return new Order(
            sequence,
            "ORD-%04d".formatted(sequence),
            customerName,
            productName,
            status,
            paymentMethod,
            amount,
            couponCode,
            address,
            risk,
            createdAt,
            updatedAt
        );
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
        this.risk = status == OrderStatus.ON_HOLD ? OrderRisk.URGENT : calculateRisk(paymentMethod, amount);
        this.updatedAt = LocalDateTime.now();
    }

    private static String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim();
    }

    private static OrderRisk calculateRisk(PaymentMethod paymentMethod, long amount) {
        if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
            return OrderRisk.ATTENTION;
        }
        if (amount >= 100_000) {
            return OrderRisk.ATTENTION;
        }
        return OrderRisk.NORMAL;
    }
}
