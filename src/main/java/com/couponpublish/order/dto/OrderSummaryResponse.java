package com.couponpublish.order.dto;

public record OrderSummaryResponse(
    long activeOrderCount,
    long paymentConfirmedCount,
    long preparingCount,
    long shippingCount,
    long completedCount,
    long onHoldCount,
    long todayRevenue
) {
}
