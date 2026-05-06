package com.example.couponpublish.coupon.infra;

import com.example.couponpublish.coupon.config.CouponProperties;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class CouponRedisRepository {

    private static final String REMAINING_KEY = "coupon:remaining";
    private static final String ISSUED_USERS_KEY = "coupon:issued-users";

    private final StringRedisTemplate redisTemplate;
    private final CouponProperties couponProperties;
    private final DefaultRedisScript<Long> issueScript;
    private final DefaultRedisScript<Long> cancelScript;

    public CouponRedisRepository(StringRedisTemplate redisTemplate, CouponProperties couponProperties) {
        this.redisTemplate = redisTemplate;
        this.couponProperties = couponProperties;
        this.issueScript = new DefaultRedisScript<>(issueLua(), Long.class);
        this.cancelScript = new DefaultRedisScript<>(cancelLua(), Long.class);
    }

    public IssueResult issue(String userId) {
        Long result = redisTemplate.execute(
            issueScript,
            List.of(REMAINING_KEY, ISSUED_USERS_KEY),
            userId,
            String.valueOf(couponProperties.maxCount())
        );
        return IssueResult.from(result);
    }

    public void rollbackIssue(String userId) {
        redisTemplate.execute(cancelScript, List.of(REMAINING_KEY, ISSUED_USERS_KEY), userId);
    }

    public void cancel(String userId) {
        redisTemplate.execute(cancelScript, List.of(REMAINING_KEY, ISSUED_USERS_KEY), userId);
    }

    public long getRemainingCount() {
        String value = redisTemplate.opsForValue().get(REMAINING_KEY);
        if (value == null) {
            return couponProperties.maxCount();
        }
        return Long.parseLong(value);
    }

    public void resetFromActiveUsers(Set<String> activeUserIds) {
        redisTemplate.delete(List.of(REMAINING_KEY, ISSUED_USERS_KEY));
        int remaining = Math.max(couponProperties.maxCount() - activeUserIds.size(), 0);
        redisTemplate.opsForValue().set(REMAINING_KEY, String.valueOf(remaining));
        if (!activeUserIds.isEmpty()) {
            redisTemplate.opsForSet().add(ISSUED_USERS_KEY, activeUserIds.toArray(String[]::new));
        }
    }

    private static String issueLua() {
        return """
            local remainingKey = KEYS[1]
            local usersKey = KEYS[2]
            local userId = ARGV[1]
            local maxCount = tonumber(ARGV[2])

            if redis.call('SISMEMBER', usersKey, userId) == 1 then
                return 1
            end

            local remaining = redis.call('GET', remainingKey)
            if not remaining then
                remaining = maxCount
                redis.call('SET', remainingKey, remaining)
            else
                remaining = tonumber(remaining)
            end

            if remaining <= 0 then
                return 2
            end

            redis.call('DECR', remainingKey)
            redis.call('SADD', usersKey, userId)
            return 0
            """;
    }

    private static String cancelLua() {
        return """
            local remainingKey = KEYS[1]
            local usersKey = KEYS[2]
            local userId = ARGV[1]

            if redis.call('SREM', usersKey, userId) == 1 then
                redis.call('INCR', remainingKey)
                return 1
            end

            return 0
            """;
    }

    public enum IssueResult {
        SUCCESS,
        DUPLICATED,
        SOLD_OUT;

        static IssueResult from(Long value) {
            if (value == null || value == 0) {
                return SUCCESS;
            }
            if (value == 1) {
                return DUPLICATED;
            }
            return SOLD_OUT;
        }
    }
}
