package com.couponpublish.order.controller;

import com.couponpublish.order.dto.OrderCreateRequest;
import com.couponpublish.order.dto.OrderEventResponse;
import com.couponpublish.order.dto.OrderPageResponse;
import com.couponpublish.order.dto.OrderResponse;
import com.couponpublish.order.dto.OrderStatusUpdateRequest;
import com.couponpublish.order.dto.OrderSummaryResponse;
import com.couponpublish.order.entity.OrderStatus;
import com.couponpublish.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request);
    }

    @GetMapping
    public OrderPageResponse getOrders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) String query,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return orderService.getOrders(status, query, page, size);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateStatus(
        @PathVariable String orderId,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return orderService.updateStatus(orderId, request);
    }

    @GetMapping("/summary")
    public OrderSummaryResponse getSummary() {
        return orderService.getSummary();
    }

    @GetMapping("/events")
    public List<OrderEventResponse> getRecentEvents(
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit
    ) {
        return orderService.getRecentEvents(limit);
    }
}
