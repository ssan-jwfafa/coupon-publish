package com.couponpublish.order.repository;

import com.couponpublish.order.entity.Order;
import com.couponpublish.order.entity.OrderStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderRepository {

    private static final String ORDER_ID_SEQUENCE_KEY = "order:id-sequence";
    private static final String ORDER_IDS_KEY = "order:ids";
    private static final String ORDER_KEY_FORMAT = "order:%s:data";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OrderRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Order save(Order order) {
        Order saved = order;
        if (saved.getOrderId() == null) {
            Long sequence = redisTemplate.opsForValue().increment(ORDER_ID_SEQUENCE_KEY);
            saved = order.withIdentity(sequence);
        }

        redisTemplate.opsForValue().set(orderKey(saved.getOrderId()), write(saved));
        redisTemplate.opsForSet().add(ORDER_IDS_KEY, saved.getOrderId());
        return saved;
    }

    public Optional<Order> findById(String orderId) {
        String value = redisTemplate.opsForValue().get(orderKey(orderId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(read(value));
    }

    public Page<Order> findAll(OrderStatus status, String query, Pageable pageable) {
        List<Order> orders = allOrders().stream()
            .filter(order -> status == null || order.getStatus() == status)
            .filter(order -> matchesQuery(order, query))
            .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
            .toList();
        return page(orders, pageable);
    }

    public List<Order> findAll() {
        return allOrders().stream()
            .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
            .toList();
    }

    public void deleteAll() {
        Set<String> orderIds = redisTemplate.opsForSet().members(ORDER_IDS_KEY);
        if (orderIds != null) {
            for (String orderId : orderIds) {
                redisTemplate.delete(orderKey(orderId));
            }
        }
        redisTemplate.delete(ORDER_IDS_KEY);
        redisTemplate.delete(ORDER_ID_SEQUENCE_KEY);
    }

    private List<Order> allOrders() {
        Set<String> orderIds = redisTemplate.opsForSet().members(ORDER_IDS_KEY);
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return orderIds.stream()
            .flatMap(orderId -> findById(orderId).stream())
            .toList();
    }

    private static boolean matchesQuery(Order order, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalized = query.trim().toLowerCase();
        return order.getOrderId().toLowerCase().contains(normalized)
            || order.getCustomerName().toLowerCase().contains(normalized)
            || order.getProductName().toLowerCase().contains(normalized);
    }

    private static Page<Order> page(List<Order> orders, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), orders.size());
        List<Order> contents = start >= orders.size() ? List.of() : orders.subList(start, end);
        return new PageImpl<>(contents, pageable, orders.size());
    }

    private Order read(String value) {
        try {
            return objectMapper.readValue(value, Order.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("주문을 읽을 수 없습니다.", ex);
        }
    }

    private String write(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("주문을 저장할 수 없습니다.", ex);
        }
    }

    private static String orderKey(String orderId) {
        return ORDER_KEY_FORMAT.formatted(orderId);
    }
}
