package com.example.couponpublish.coupon.repository;

import com.example.couponpublish.coupon.entity.Coupon;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CouponRepository {

    private static final String COUPON_ID_SEQUENCE_KEY = "coupon:id-sequence";
    private static final String COUPON_IDS_KEY = "coupon:ids";
    private static final String COUPON_KEY_FORMAT = "coupon:%d:data";

    private final StringRedisTemplate redisTemplate;

    public CouponRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Coupon save(Coupon coupon) {
        Long couponId = coupon.getId();
        if (couponId == null) {
            couponId = redisTemplate.opsForValue().increment(COUPON_ID_SEQUENCE_KEY);
        }

        Coupon saved = coupon.withId(couponId);
        redisTemplate.opsForHash().putAll(couponKey(couponId), Map.of(
            "id", String.valueOf(saved.getId()),
            "name", saved.getName(),
            "maxCount", String.valueOf(saved.getMaxCount()),
            "startAt", saved.getStartAt().toString(),
            "endAt", saved.getEndAt().toString()
        ));
        redisTemplate.opsForSet().add(COUPON_IDS_KEY, String.valueOf(couponId));
        return saved;
    }

    public Optional<Coupon> findById(Long couponId) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(couponKey(couponId));
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toCoupon(values));
    }

    public boolean existsById(Long couponId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(couponKey(couponId)));
    }

    public List<Coupon> findAll() {
        return redisTemplate.opsForSet().members(COUPON_IDS_KEY).stream()
            .map(Long::valueOf)
            .flatMap(couponId -> findById(couponId).stream())
            .sorted(Comparator.comparing(Coupon::getId).reversed())
            .toList();
    }

    public void deleteAll() {
        for (String couponId : redisTemplate.opsForSet().members(COUPON_IDS_KEY)) {
            redisTemplate.delete(couponKey(Long.valueOf(couponId)));
        }
        redisTemplate.delete(COUPON_IDS_KEY);
        redisTemplate.delete(COUPON_ID_SEQUENCE_KEY);
    }

    public void deleteById(Long couponId) {
        redisTemplate.delete(couponKey(couponId));
        redisTemplate.opsForSet().remove(COUPON_IDS_KEY, String.valueOf(couponId));
    }

    private static Coupon toCoupon(Map<Object, Object> values) {
        return Coupon.create(
            string(values, "name"),
            Integer.parseInt(string(values, "maxCount")),
            LocalDateTime.parse(string(values, "startAt")),
            LocalDateTime.parse(string(values, "endAt"))
        ).withId(Long.valueOf(string(values, "id")));
    }

    private static String string(Map<Object, Object> values, String key) {
        return String.valueOf(values.get(key));
    }

    private static String couponKey(Long couponId) {
        return COUPON_KEY_FORMAT.formatted(couponId);
    }
}
