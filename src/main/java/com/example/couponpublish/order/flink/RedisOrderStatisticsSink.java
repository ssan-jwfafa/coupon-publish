package com.example.couponpublish.order.flink;

import com.example.couponpublish.order.entity.OrderEventType;
import com.example.couponpublish.order.entity.OrderStatus;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import redis.clients.jedis.Jedis;

public class RedisOrderStatisticsSink implements Sink<OrderLifecycleStatisticsEvent> {

    private static final String SUMMARY_KEY = "order:statistics:summary";
    private static final String EVENTS_DEDUP_KEY = "order:statistics:events";
    private static final String RECENT_EVENTS_KEY = "order:statistics:recent-events";

    private final String redisHost;
    private final int redisPort;

    public RedisOrderStatisticsSink(String redisHost, int redisPort) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    @Override
    public SinkWriter<OrderLifecycleStatisticsEvent> createWriter(WriterInitContext context) {
        return new RedisOrderStatisticsWriter(redisHost, redisPort);
    }

    @Override
    @SuppressWarnings("deprecation")
    public SinkWriter<OrderLifecycleStatisticsEvent> createWriter(InitContext context) {
        return new RedisOrderStatisticsWriter(redisHost, redisPort);
    }

    private static class RedisOrderStatisticsWriter implements SinkWriter<OrderLifecycleStatisticsEvent> {

        private final Jedis jedis;

        private RedisOrderStatisticsWriter(String redisHost, int redisPort) {
            this.jedis = new Jedis(redisHost, redisPort);
        }

        @Override
        public void write(OrderLifecycleStatisticsEvent event, Context context) {
            Long added = jedis.sadd(EVENTS_DEDUP_KEY, eventId(event));
            if (added == null || added == 0) {
                return;
            }

            if (event.getType() == OrderEventType.CREATED) {
                incrementStatus(event.getStatus(), 1);
                incrementTodayRevenue(event);
            }
            if (event.getType() == OrderEventType.STATUS_CHANGED) {
                incrementStatus(event.getPreviousStatus(), -1);
                incrementStatus(event.getStatus(), 1);
            }

            jedis.hset(SUMMARY_KEY, "updatedAt", LocalDateTime.now().toString());
            pushRecentEvent(event);
        }

        @Override
        public void flush(boolean endOfInput) {
        }

        @Override
        public void close() throws IOException {
            jedis.close();
        }

        private void incrementStatus(OrderStatus status, long delta) {
            if (status == null) {
                return;
            }
            jedis.hincrBy(SUMMARY_KEY, statusField(status), delta);
        }

        private void incrementTodayRevenue(OrderLifecycleStatisticsEvent event) {
            LocalDate occurredDate = LocalDateTime.parse(event.getOccurredAt()).toLocalDate();
            LocalDate today = LocalDate.now();
            if (!occurredDate.equals(today)) {
                return;
            }

            String currentRevenueDate = jedis.hget(SUMMARY_KEY, "revenueDate");
            if (!today.toString().equals(currentRevenueDate)) {
                jedis.hset(SUMMARY_KEY, "revenueDate", today.toString());
                jedis.hset(SUMMARY_KEY, "todayRevenue", "0");
            }
            jedis.hincrBy(SUMMARY_KEY, "todayRevenue", event.getAmount());
        }

        private void pushRecentEvent(OrderLifecycleStatisticsEvent event) {
            jedis.lpush(RECENT_EVENTS_KEY, eventJson(event));
            jedis.ltrim(RECENT_EVENTS_KEY, 0, 49);
        }
    }

    private static String statusField(OrderStatus status) {
        return switch (status) {
            case PAYMENT_CONFIRMED -> "paymentConfirmedCount";
            case PREPARING -> "preparingCount";
            case SHIPPING -> "shippingCount";
            case COMPLETED -> "completedCount";
            case ON_HOLD -> "onHoldCount";
        };
    }

    private static String eventId(OrderLifecycleStatisticsEvent event) {
        if (event.getEventId() != null) {
            return String.valueOf(event.getEventId());
        }
        return event.getOrderId() + ":" + event.getType() + ":" + event.getOccurredAt();
    }

    private static String eventJson(OrderLifecycleStatisticsEvent event) {
        return """
            {"eventId":%s,"orderId":"%s","type":"%s","message":"%s","occurredAt":"%s"}\
            """.formatted(
            event.getEventId(),
            escape(event.getOrderId()),
            event.getType(),
            escape(event.getMessage()),
            event.getOccurredAt()
        );
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
