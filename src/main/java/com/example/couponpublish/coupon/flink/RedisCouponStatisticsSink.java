package com.example.couponpublish.coupon.flink;

import com.example.couponpublish.coupon.event.CouponIssuedEvent;
import java.time.LocalDateTime;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import redis.clients.jedis.Jedis;

public class RedisCouponStatisticsSink extends RichSinkFunction<CouponIssuedEvent> {

    private static final String STATISTICS_KEY_FORMAT = "coupon:%d:statistics";
    private static final String STATISTICS_EVENTS_KEY_FORMAT = "coupon:%d:statistics-events";

    private final String redisHost;
    private final int redisPort;
    private transient Jedis jedis;

    public RedisCouponStatisticsSink(String redisHost, int redisPort) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    @Override
    public void open(Configuration parameters) {
        this.jedis = new Jedis(redisHost, redisPort);
    }

    @Override
    public void invoke(CouponIssuedEvent event, Context context) {
        String eventId = eventId(event);
        String eventsKey = statisticsEventsKey(event.couponId());
        Long added = jedis.sadd(eventsKey, eventId);
        if (added == null || added == 0) {
            return;
        }

        String statisticsKey = statisticsKey(event.couponId());
        jedis.hincrBy(statisticsKey, "issuedCount", 1);
        updateLastIssuedAt(statisticsKey, event.issuedAt());
        jedis.hset(statisticsKey, "updatedAt", LocalDateTime.now().toString());
    }

    @Override
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }

    private void updateLastIssuedAt(String statisticsKey, LocalDateTime issuedAt) {
        String current = jedis.hget(statisticsKey, "lastIssuedAt");
        if (current == null || issuedAt.isAfter(LocalDateTime.parse(current))) {
            jedis.hset(statisticsKey, "lastIssuedAt", issuedAt.toString());
        }
    }

    private static String eventId(CouponIssuedEvent event) {
        if (event.couponIssueId() != null) {
            return String.valueOf(event.couponIssueId());
        }
        return event.couponId() + ":" + event.userId() + ":" + event.issuedAt();
    }

    private static String statisticsKey(Long couponId) {
        return STATISTICS_KEY_FORMAT.formatted(couponId);
    }

    private static String statisticsEventsKey(Long couponId) {
        return STATISTICS_EVENTS_KEY_FORMAT.formatted(couponId);
    }
}
