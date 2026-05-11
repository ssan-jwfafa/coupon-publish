package com.example.couponpublish.coupon.event;

import com.example.couponpublish.coupon.service.CouponIssueStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "coupon.kafka", name = "enabled", havingValue = "true")
public class CouponIssuedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CouponIssuedEventConsumer.class);

    private final CouponIssueStatisticsService statisticsService;

    public CouponIssuedEventConsumer(CouponIssueStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @KafkaListener(topics = "${coupon.kafka.topics.coupon-issued}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(CouponIssuedEvent event) {
        statisticsService.aggregateIssuedEvent(event);
        log.info(
            "coupon issued event aggregated: couponIssueId={}, couponId={}, userId={}",
            event.couponIssueId(),
            event.couponId(),
            event.userId()
        );
    }
}
