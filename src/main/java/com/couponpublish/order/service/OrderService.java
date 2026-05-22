package com.couponpublish.order.service;

import com.couponpublish.order.dto.OrderCreateRequest;
import com.couponpublish.order.dto.OrderEventResponse;
import com.couponpublish.order.dto.OrderPageResponse;
import com.couponpublish.order.dto.OrderResponse;
import com.couponpublish.order.dto.OrderStatusUpdateRequest;
import com.couponpublish.order.dto.OrderSummaryResponse;
import com.couponpublish.order.entity.Order;
import com.couponpublish.order.entity.OrderEvent;
import com.couponpublish.order.entity.OrderEventType;
import com.couponpublish.order.entity.OrderStatus;
import com.couponpublish.order.event.OrderEventPublisher;
import com.couponpublish.order.event.OrderLifecycleEvent;
import com.couponpublish.order.exception.OrderException;
import com.couponpublish.order.repository.OrderEventRepository;
import com.couponpublish.order.repository.OrderRepository;
import com.couponpublish.order.repository.OrderStatisticsRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderStatisticsRepository orderStatisticsRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(
        OrderRepository orderRepository,
        OrderEventRepository orderEventRepository,
        OrderStatisticsRepository orderStatisticsRepository,
        OrderEventPublisher orderEventPublisher
    ) {
        this.orderRepository = orderRepository;
        this.orderEventRepository = orderEventRepository;
        this.orderStatisticsRepository = orderStatisticsRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    public OrderResponse createOrder(OrderCreateRequest request) {
        Order order = Order.create(
            request.customerName(),
            request.productName(),
            request.paymentMethod(),
            request.amount(),
            request.couponCode(),
            request.address()
        );
        Order saved = orderRepository.save(order);
        OrderEvent event = orderEventRepository.save(OrderEvent.create(
            saved.getOrderId(),
            OrderEventType.CREATED,
            "%s 주문이 접수되었습니다.".formatted(saved.getOrderId())
        ));
        orderEventPublisher.publish(OrderLifecycleEvent.created(saved, event));
        return OrderResponse.from(saved);
    }

    public OrderResponse getOrder(String orderId) {
        return OrderResponse.from(getOrderOrThrow(orderId));
    }

    public OrderPageResponse getOrders(OrderStatus status, String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return OrderPageResponse.from(orderRepository.findAll(status, query, pageRequest));
    }

    public OrderResponse updateStatus(String orderId, OrderStatusUpdateRequest request) {
        Order order = getOrderOrThrow(orderId);
        OrderStatus previousStatus = order.getStatus();
        order.changeStatus(request.status());
        Order saved = orderRepository.save(order);
        OrderEvent event = orderEventRepository.save(OrderEvent.create(
            saved.getOrderId(),
            OrderEventType.STATUS_CHANGED,
            "%s 상태가 %s에서 %s로 변경되었습니다.".formatted(saved.getOrderId(), previousStatus, saved.getStatus())
        ));
        orderEventPublisher.publish(OrderLifecycleEvent.statusChanged(saved, event, previousStatus));
        return OrderResponse.from(saved);
    }

    public OrderSummaryResponse getSummary() {
        return orderStatisticsRepository.findSummary()
            .orElseGet(this::calculateSummaryFromOrders);
    }

    public List<OrderEventResponse> getRecentEvents(int limit) {
        List<OrderEvent> events = orderStatisticsRepository.findRecentEvents(limit);
        if (events.isEmpty()) {
            events = orderEventRepository.findRecent(limit);
        }
        return events.stream()
            .map(OrderEventResponse::from)
            .toList();
    }

    private OrderSummaryResponse calculateSummaryFromOrders() {
        List<Order> orders = orderRepository.findAll();
        long paymentConfirmedCount = countByStatus(orders, OrderStatus.PAYMENT_CONFIRMED);
        long preparingCount = countByStatus(orders, OrderStatus.PREPARING);
        long shippingCount = countByStatus(orders, OrderStatus.SHIPPING);
        long completedCount = countByStatus(orders, OrderStatus.COMPLETED);
        long onHoldCount = countByStatus(orders, OrderStatus.ON_HOLD);
        long todayRevenue = orders.stream()
            .filter(order -> order.getCreatedAt().toLocalDate().equals(LocalDate.now()))
            .mapToLong(Order::getAmount)
            .sum();

        return new OrderSummaryResponse(
            paymentConfirmedCount + preparingCount + shippingCount + onHoldCount,
            paymentConfirmedCount,
            preparingCount,
            shippingCount,
            completedCount,
            onHoldCount,
            todayRevenue
        );
    }

    private Order getOrderOrThrow(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException(HttpStatus.NOT_FOUND, "주문이 없습니다."));
    }

    private static long countByStatus(List<Order> orders, OrderStatus status) {
        return orders.stream()
            .filter(order -> order.getStatus() == status)
            .count();
    }
}
