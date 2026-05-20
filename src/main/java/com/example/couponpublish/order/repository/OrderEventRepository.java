package com.example.couponpublish.order.repository;

import com.example.couponpublish.order.entity.OrderEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventRepository {

    private static final String ORDER_EVENT_ID_SEQUENCE_KEY = "order-event:id-sequence";
    private static final String ORDER_EVENTS_KEY = "order:events";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public OrderEvent save(OrderEvent event) {
        OrderEvent saved = assignId(event);
        redisTemplate.opsForList().leftPush(ORDER_EVENTS_KEY, write(saved));
        redisTemplate.opsForList().trim(ORDER_EVENTS_KEY, 0, 49);
        return saved;
    }

    public OrderEvent assignId(OrderEvent event) {
        Long eventId = redisTemplate.opsForValue().increment(ORDER_EVENT_ID_SEQUENCE_KEY);
        return event.withId(eventId);
    }

    public List<OrderEvent> findRecent(int limit) {
        long end = Math.max(0, limit - 1L);
        return Optional.ofNullable(redisTemplate.opsForList().range(ORDER_EVENTS_KEY, 0, end))
            .orElseGet(List::of)
            .stream()
            .map(this::read)
            .toList();
    }

    public void deleteAll() {
        redisTemplate.delete(ORDER_EVENTS_KEY);
        redisTemplate.delete(ORDER_EVENT_ID_SEQUENCE_KEY);
    }

    private OrderEvent read(String value) {
        try {
            return objectMapper.readValue(value, OrderEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("주문 이벤트를 읽을 수 없습니다.", ex);
        }
    }

    private String write(OrderEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("주문 이벤트를 저장할 수 없습니다.", ex);
        }
    }
}
