package com.example.couponpublish.coupon.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CouponStatisticsFlinkJob {

    private static final Logger log = LoggerFactory.getLogger(CouponStatisticsFlinkJob.class);
    private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String DEFAULT_TOPIC = "coupon-issued";
    private static final String DEFAULT_GROUP_ID = "coupon-flink-statistics";
    private static final String DEFAULT_REDIS_HOST = "localhost";
    private static final int DEFAULT_REDIS_PORT = 6379;

    private final String bootstrapServers;
    private final String redisHost;
    private final int redisPort;
    private final String topic;
    private final String consumerGroupId;

    public CouponStatisticsFlinkJob(
        String bootstrapServers,
        String redisHost,
        int redisPort,
        String topic,
        String consumerGroupId
    ) {
        this.bootstrapServers = bootstrapServers;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.topic = topic;
        this.consumerGroupId = consumerGroupId;
    }

    public static void main(String[] args) {
        new CouponStatisticsFlinkJob(
            property("coupon.flink.kafka.bootstrap-servers", DEFAULT_BOOTSTRAP_SERVERS),
            property("coupon.flink.redis.host", DEFAULT_REDIS_HOST),
            Integer.parseInt(property("coupon.flink.redis.port", String.valueOf(DEFAULT_REDIS_PORT))),
            property("coupon.flink.topic", DEFAULT_TOPIC),
            property("coupon.flink.consumer-group-id", DEFAULT_GROUP_ID)
        ).run();
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                execute();
            } catch (Exception ex) {
                log.error("coupon statistics flink job stopped. retrying in 5 seconds", ex);
                sleepBeforeRetry();
            }
        }
    }

    private void execute() throws Exception {
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        environment.setParallelism(1);

        KafkaSource<CouponIssuedStatisticsEvent> source = KafkaSource.<CouponIssuedStatisticsEvent>builder()
            .setBootstrapServers(bootstrapServers)
            .setTopics(topic)
            .setGroupId(consumerGroupId)
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new CouponIssuedEventDeserializationSchema())
            .build();

        environment.fromSource(source, WatermarkStrategy.noWatermarks(), "coupon-issued-source")
            .keyBy(CouponIssuedStatisticsEvent::getCouponId)
            .addSink(new RedisCouponStatisticsSink(redisHost, redisPort))
            .name("redis-coupon-statistics-sink");

        environment.execute("coupon-issued-statistics");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(5_000);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private static String property(String key, String defaultValue) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }

        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }

        return defaultValue;
    }
}
