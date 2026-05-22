package com.couponpublish.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.couponpublish.coupon.entity.Coupon;
import com.couponpublish.coupon.entity.CouponIssue;
import com.couponpublish.coupon.entity.CouponStatus;
import com.couponpublish.coupon.exception.CouponException;
import com.couponpublish.coupon.repository.CouponIssueRepository;
import com.couponpublish.coupon.repository.CouponRedisRepository;
import com.couponpublish.coupon.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CouponServiceConcurrencyTest {

    private static final int MAX_COUPON_COUNT = 100;

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
        .withExposedPorts(6379);

    @Autowired
    CouponService couponService;

    @Autowired
    CouponRepository couponRepository;

    @Autowired
    CouponIssueRepository couponIssueRepository;

    @Autowired
    CouponRedisRepository couponRedisRepository;

    @Autowired
    CouponRedisInitializer couponRedisInitializer;

    Coupon coupon;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:coupon_publish;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("coupon.kafka.enabled", () -> false);
        registry.add("order.kafka.enabled", () -> false);
        registry.add("outbox.relay.enabled", () -> false);
    }

    @BeforeEach
    void setUp() {
        couponIssueRepository.deleteAll();
        couponRepository.deleteAll();
        coupon = couponRepository.save(Coupon.create(
            "test coupon",
            MAX_COUPON_COUNT,
            LocalDateTime.now().minusMinutes(1),
            LocalDateTime.now().plusHours(1)
        ));
        couponRedisRepository.resetFromActiveUsers(coupon.getId(), coupon.getMaxCount(), Set.of());
    }

    @Test
    void only100RequestsSucceedWhen1000UsersRequestAtTheSameTime() throws Exception {
        long successCount = issueConcurrently(1_000, index -> "user-" + index);

        assertThat(successCount).isEqualTo(MAX_COUPON_COUNT);
        assertThat(couponIssueRepository.countByCoupon_IdAndStatus(coupon.getId(), CouponStatus.ISSUED))
            .isEqualTo(MAX_COUPON_COUNT);
        assertThat(couponRedisRepository.getRemainingCount(coupon.getId(), coupon.getMaxCount())).isZero();
    }

    @Test
    void onlyOneRequestSucceedsWhenSameUserRequests100TimesAtTheSameTime() throws Exception {
        long successCount = issueConcurrently(100, ignored -> "same-user");

        assertThat(successCount).isOne();
        assertThat(couponIssueRepository.countByCoupon_IdAndStatus(coupon.getId(), CouponStatus.ISSUED)).isOne();
        assertThat(couponRedisRepository.getRemainingCount(coupon.getId(), coupon.getMaxCount()))
            .isEqualTo(MAX_COUPON_COUNT - 1);
    }

    @Test
    void recoverRedisFromDatabaseIssuedHistoryWhenServerRestarts() {
        CouponIssue canceled = CouponIssue.issue(coupon, "canceled-user");
        canceled.cancel();
        couponIssueRepository.save(CouponIssue.issue(coupon, "active-user-1"));
        couponIssueRepository.save(CouponIssue.issue(coupon, "active-user-2"));
        couponIssueRepository.save(canceled);
        couponRedisRepository.resetFromActiveUsers(coupon.getId(), coupon.getMaxCount(), Set.of("stale-user"));

        couponRedisInitializer.run(null);

        assertThat(couponRedisRepository.getRemainingCount(coupon.getId(), coupon.getMaxCount()))
            .isEqualTo(MAX_COUPON_COUNT - 2);
        assertThatThrownBy(() -> couponService.issue(coupon.getId(), "active-user-1"))
            .isInstanceOf(CouponException.class)
            .hasMessage("이미 발급된 사용자입니다.");
        assertThat(couponRedisRepository.getRemainingCount(coupon.getId(), coupon.getMaxCount()))
            .isEqualTo(MAX_COUPON_COUNT - 2);
    }

    private long issueConcurrently(int requestCount, IntFunction<String> userIdProvider) throws Exception {
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int index = 0; index < requestCount; index++) {
                int requestIndex = index;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        couponService.issue(coupon.getId(), userIdProvider.apply(requestIndex));
                        return true;
                    } catch (CouponException ex) {
                        return false;
                    }
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long successCount = 0;
            for (Future<Boolean> future : futures) {
                if (getFutureResult(future)) {
                    successCount++;
                }
            }
            return successCount;
        }
    }

    private boolean getFutureResult(Future<Boolean> future) throws Exception {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            throw new AssertionError("동시성 테스트 요청 중 예상하지 못한 예외가 발생했습니다.", ex.getCause());
        } catch (TimeoutException ex) {
            throw new AssertionError("동시성 테스트 요청이 제한 시간 안에 끝나지 않았습니다.", ex);
        }
    }
}
