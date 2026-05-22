package com.couponpublish.coupon.repository;

import com.couponpublish.coupon.entity.CouponIssueStatistics;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CouponIssueStatisticsRepository {

    private static final String STATISTICS_KEY_FORMAT = "coupon:%d:statistics";
    private static final String STATISTICS_EVENTS_KEY_FORMAT = "coupon:%d:statistics-events";

    private final StringRedisTemplate redisTemplate;

    public CouponIssueStatisticsRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<CouponIssueStatistics> findByCouponId(Long couponId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(statisticsKey(couponId));
        if (values.isEmpty()) {
            return Optional.empty();
        }

        String lastIssuedAt = string(values, "lastIssuedAt");
        String updatedAt = string(values, "updatedAt");
        return Optional.of(new CouponIssueStatistics(
            couponId,
            Long.parseLong(string(values, "issuedCount")),
            lastIssuedAt == null ? null : LocalDateTime.parse(lastIssuedAt),
            updatedAt == null ? null : LocalDateTime.parse(updatedAt)
        ));
    }

    public void save(CouponIssueStatistics statistics) {
        redisTemplate.opsForHash().putAll(statisticsKey(statistics.getCouponId()), Map.of(
            "issuedCount", String.valueOf(statistics.getIssuedCount()),
            "lastIssuedAt", statistics.getLastIssuedAt().toString(),
            "updatedAt", statistics.getUpdatedAt().toString()
        ));
    }

    public void deleteByCouponId(Long couponId) {
        redisTemplate.delete(List.of(statisticsKey(couponId), statisticsEventsKey(couponId)));
    }

    private static String string(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String statisticsKey(Long couponId) {
        return STATISTICS_KEY_FORMAT.formatted(couponId);
    }

    private static String statisticsEventsKey(Long couponId) {
        return STATISTICS_EVENTS_KEY_FORMAT.formatted(couponId);
    }
}
