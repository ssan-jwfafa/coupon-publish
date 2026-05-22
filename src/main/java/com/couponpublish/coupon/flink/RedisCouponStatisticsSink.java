package com.couponpublish.coupon.flink;

import java.io.IOException;
import java.time.LocalDateTime;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import redis.clients.jedis.Jedis;

public class RedisCouponStatisticsSink implements Sink<CouponIssuedStatisticsEvent> {

    private static final String STATISTICS_KEY_FORMAT = "coupon:%d:statistics";
    private static final String STATISTICS_EVENTS_KEY_FORMAT = "coupon:%d:statistics-events";

    private final String redisHost;
    private final int redisPort;

    public RedisCouponStatisticsSink(String redisHost, int redisPort) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    @Override
    public SinkWriter<CouponIssuedStatisticsEvent> createWriter(WriterInitContext context) {
        return new RedisCouponStatisticsWriter(redisHost, redisPort);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SinkWriter<CouponIssuedStatisticsEvent> createWriter(InitContext context) {
        return new RedisCouponStatisticsWriter(redisHost, redisPort);
    }

    private static class RedisCouponStatisticsWriter implements SinkWriter<CouponIssuedStatisticsEvent> {

        private final Jedis jedis;

        private RedisCouponStatisticsWriter(String redisHost, int redisPort) {
            this.jedis = new Jedis(redisHost, redisPort);
        }

        @Override
        public void write(CouponIssuedStatisticsEvent event, Context context) {
            String eventId = eventId(event);
            String eventsKey = statisticsEventsKey(event.getCouponId());
            Long added = jedis.sadd(eventsKey, eventId);
            if (added == null || added == 0) {
                return;
            }

            String statisticsKey = statisticsKey(event.getCouponId());
            jedis.hincrBy(statisticsKey, "issuedCount", 1);
            updateLastIssuedAt(statisticsKey, LocalDateTime.parse(event.getIssuedAt()));
            jedis.hset(statisticsKey, "updatedAt", LocalDateTime.now().toString());
        }

        @Override
        public void flush(boolean endOfInput) {
        }

        @Override
        public void close() throws IOException {
            jedis.close();
        }

        private void updateLastIssuedAt(String statisticsKey, LocalDateTime issuedAt) {
            String current = jedis.hget(statisticsKey, "lastIssuedAt");
            if (current == null || issuedAt.isAfter(LocalDateTime.parse(current))) {
                jedis.hset(statisticsKey, "lastIssuedAt", issuedAt.toString());
            }
        }
    }

    private static String eventId(CouponIssuedStatisticsEvent event) {
        if (event.getCouponIssueId() != null) {
            return String.valueOf(event.getCouponIssueId());
        }
        return event.getCouponId() + ":" + event.getUserId() + ":" + event.getIssuedAt();
    }

    private static String statisticsKey(Long couponId) {
        return STATISTICS_KEY_FORMAT.formatted(couponId);
    }

    private static String statisticsEventsKey(Long couponId) {
        return STATISTICS_EVENTS_KEY_FORMAT.formatted(couponId);
    }
}
