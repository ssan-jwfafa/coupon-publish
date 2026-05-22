package com.couponpublish.order.dto;

import com.couponpublish.order.entity.Order;
import java.util.List;
import org.springframework.data.domain.Page;

public record OrderPageResponse(
    List<OrderResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    public static OrderPageResponse from(Page<Order> page) {
        return new OrderPageResponse(
            page.getContent().stream().map(OrderResponse::from).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
