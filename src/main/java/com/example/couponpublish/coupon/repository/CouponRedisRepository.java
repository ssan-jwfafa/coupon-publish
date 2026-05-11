package com.example.couponpublish.coupon.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class CouponRedisRepository {

    private static final String REMAINING_KEY_FORMAT = "coupon:%d:remaining";
    private static final String ISSUED_USERS_KEY_FORMAT = "coupon:%d:issued-users";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> issueScript;
    private final DefaultRedisScript<Long> cancelScript;

    public CouponRedisRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.issueScript = new DefaultRedisScript<>(issueLua(), Long.class);
        this.cancelScript = new DefaultRedisScript<>(cancelLua(), Long.class);
    }

    public IssueResult issue(Long couponId, String userId, int maxCount) {
        Long result = redisTemplate.execute(
            issueScript,
            List.of(remainingKey(couponId), issuedUsersKey(couponId)),
            userId,
            String.valueOf(maxCount)
        );
        return IssueResult.from(result);
    }

    public void rollbackIssue(Long couponId, String userId) {
        redisTemplate.execute(cancelScript, List.of(remainingKey(couponId), issuedUsersKey(couponId)), userId);
    }

    public void cancel(Long couponId, String userId) {
        redisTemplate.execute(cancelScript, List.of(remainingKey(couponId), issuedUsersKey(couponId)), userId);
    }

    public long getRemainingCount(Long couponId, int maxCount) {
        String value = redisTemplate.opsForValue().get(remainingKey(couponId));
        if (value == null) {
            return maxCount;
        }
        return Long.parseLong(value);
    }

    public void resetFromActiveUsers(Long couponId, int maxCount, Set<String> activeUserIds) {
        String remainingKey = remainingKey(couponId);
        String issuedUsersKey = issuedUsersKey(couponId);
        redisTemplate.delete(List.of(remainingKey, issuedUsersKey));
        int remaining = Math.max(maxCount - activeUserIds.size(), 0);
        redisTemplate.opsForValue().set(remainingKey, String.valueOf(remaining));
        if (!activeUserIds.isEmpty()) {
            redisTemplate.opsForSet().add(issuedUsersKey, activeUserIds.toArray(String[]::new));
        }
    }

    private static String remainingKey(Long couponId) {
        return REMAINING_KEY_FORMAT.formatted(couponId);
    }

    private static String issuedUsersKey(Long couponId) {
        return ISSUED_USERS_KEY_FORMAT.formatted(couponId);
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
