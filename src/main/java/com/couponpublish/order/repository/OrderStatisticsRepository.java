package com.couponpublish.order.repository;

import com.couponpublish.order.dto.OrderSummaryResponse;
import com.couponpublish.order.entity.OrderEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderStatisticsRepository {

    private static final String SUMMARY_KEY = "order:statistics:summary";
    private static final String RECENT_EVENTS_KEY = "order:statistics:recent-events";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OrderStatisticsRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<OrderSummaryResponse> findSummary() {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(SUMMARY_KEY);
        if (values.isEmpty()) {
            return Optional.empty();
        }

        long paymentConfirmedCount = longValue(values, "paymentConfirmedCount");
        long preparingCount = longValue(values, "preparingCount");
        long shippingCount = longValue(values, "shippingCount");
        long completedCount = longValue(values, "completedCount");
        long onHoldCount = longValue(values, "onHoldCount");
        long todayRevenue = todayRevenue(values);

        return Optional.of(new OrderSummaryResponse(
            paymentConfirmedCount + preparingCount + shippingCount + onHoldCount,
            paymentConfirmedCount,
            preparingCount,
            shippingCount,
            completedCount,
            onHoldCount,
            todayRevenue
        ));
    }

    public List<OrderEvent> findRecentEvents(int limit) {
        long end = Math.max(0, limit - 1L);
        return Optional.ofNullable(redisTemplate.opsForList().range(RECENT_EVENTS_KEY, 0, end))
            .orElseGet(List::of)
            .stream()
            .map(this::read)
            .toList();
    }

    private static long todayRevenue(Map<Object, Object> values) {
        String revenueDate = string(values, "revenueDate");
        if (!LocalDate.now().toString().equals(revenueDate)) {
            return 0;
        }
        return longValue(values, "todayRevenue");
    }

    private static long longValue(Map<Object, Object> values, String key) {
        String value = string(values, key);
        return value == null ? 0 : Long.parseLong(value);
    }

    private static String string(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private OrderEvent read(String value) {
        try {
            return objectMapper.readValue(value, OrderEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("주문 이벤트를 읽을 수 없습니다.", ex);
        }
    }
}
