package com.example.couponpublish.coupon.flink;

import com.example.couponpublish.coupon.config.FlinkProperties;
import com.example.couponpublish.coupon.config.KafkaTopicProperties;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "coupon.flink", name = "enabled", havingValue = "true")
public class CouponStatisticsFlinkJob {

    private static final Logger log = LoggerFactory.getLogger(CouponStatisticsFlinkJob.class);

    private final String bootstrapServers;
    private final String redisHost;
    private final int redisPort;
    private final KafkaTopicProperties topicProperties;
    private final FlinkProperties flinkProperties;

    public CouponStatisticsFlinkJob(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
        @Value("${spring.data.redis.host}") String redisHost,
        @Value("${spring.data.redis.port}") int redisPort,
        KafkaTopicProperties topicProperties,
        FlinkProperties flinkProperties
    ) {
        this.bootstrapServers = bootstrapServers;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.topicProperties = topicProperties;
        this.flinkProperties = flinkProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread thread = new Thread(this::run, "coupon-statistics-flink-job");
        thread.setDaemon(false);
        thread.start();
    }

    private void run() {
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
            .setTopics(topicProperties.couponIssued())
            .setGroupId(flinkProperties.consumerGroupId())
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
}
