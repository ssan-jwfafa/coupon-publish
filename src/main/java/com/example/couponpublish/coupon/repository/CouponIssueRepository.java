package com.example.couponpublish.coupon.repository;

import com.example.couponpublish.coupon.entity.CouponIssue;
import com.example.couponpublish.coupon.entity.CouponStatus;
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
public class CouponIssueRepository {

    private static final String ISSUE_ID_SEQUENCE_KEY = "coupon-issue:id-sequence";
    private static final String ISSUE_HASH_KEY_FORMAT = "coupon:%d:issues";
    private static final String ISSUE_INDEX_KEY_FORMAT = "coupon:%d:issue-index";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CouponIssueRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public CouponIssue save(CouponIssue couponIssue) {
        Long issueId = couponIssue.getId();
        if (issueId == null) {
            issueId = redisTemplate.opsForValue().increment(ISSUE_ID_SEQUENCE_KEY);
        }

        CouponIssue saved = couponIssue.withId(issueId);
        String value = write(saved);
        redisTemplate.opsForHash().put(issueHashKey(saved.getCouponId()), saved.getUserId(), value);
        redisTemplate.opsForZSet().add(
            issueIndexKey(saved.getCouponId()),
            saved.getUserId(),
            saved.getIssuedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        );
        return saved;
    }

    public Optional<CouponIssue> findByCoupon_IdAndUserId(Long couponId, String userId) {
        Object value = redisTemplate.opsForHash().get(issueHashKey(couponId), userId);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(read(String.valueOf(value)));
    }

    public Optional<CouponIssue> findByCouponIdAndUserIdForUpdate(Long couponId, String userId) {
        return findByCoupon_IdAndUserId(couponId, userId);
    }

    public Page<CouponIssue> findAllByCoupon_Id(Long couponId, Pageable pageable) {
        List<CouponIssue> issues = allIssues(couponId);
        return page(issues, pageable);
    }

    public Page<CouponIssue> findAllByCoupon_IdAndStatus(Long couponId, CouponStatus status, Pageable pageable) {
        List<CouponIssue> issues = allIssues(couponId).stream()
            .filter(issue -> issue.getStatus() == status)
            .toList();
        return page(issues, pageable);
    }

    public List<CouponIssue> findAllByCoupon_IdAndStatus(Long couponId, CouponStatus status) {
        return allIssues(couponId).stream()
            .filter(issue -> issue.getStatus() == status)
            .toList();
    }

    public long countByCoupon_IdAndStatus(Long couponId, CouponStatus status) {
        return findAllByCoupon_IdAndStatus(couponId, status).size();
    }

    public void deleteAll() {
        deleteKeys("coupon:*:issues");
        deleteKeys("coupon:*:issue-index");
        redisTemplate.delete(ISSUE_ID_SEQUENCE_KEY);
    }

    public void deleteAllByCouponId(Long couponId) {
        redisTemplate.delete(List.of(issueHashKey(couponId), issueIndexKey(couponId)));
    }

    public void deleteByCouponIdAndUserId(Long couponId, String userId) {
        redisTemplate.opsForHash().delete(issueHashKey(couponId), userId);
        redisTemplate.opsForZSet().remove(issueIndexKey(couponId), userId);
    }

    private List<CouponIssue> allIssues(Long couponId) {
        return redisTemplate.opsForHash().values(issueHashKey(couponId)).stream()
            .map(value -> read(String.valueOf(value)))
            .sorted(Comparator.comparing(CouponIssue::getIssuedAt).reversed())
            .toList();
    }

    private static Page<CouponIssue> page(List<CouponIssue> issues, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), issues.size());
        List<CouponIssue> contents = start >= issues.size() ? List.of() : issues.subList(start, end);
        return new PageImpl<>(contents, pageable, issues.size());
    }

    private CouponIssue read(String value) {
        try {
            return objectMapper.readValue(value, CouponIssue.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("쿠폰 발급 내역을 읽을 수 없습니다.", ex);
        }
    }

    private String write(CouponIssue couponIssue) {
        try {
            return objectMapper.writeValueAsString(couponIssue);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("쿠폰 발급 내역을 저장할 수 없습니다.", ex);
        }
    }

    private void deleteKeys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private static String issueHashKey(Long couponId) {
        return ISSUE_HASH_KEY_FORMAT.formatted(couponId);
    }

    private static String issueIndexKey(Long couponId) {
        return ISSUE_INDEX_KEY_FORMAT.formatted(couponId);
    }
}
