package com.example.couponpublish.coupon.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "coupon.kafka", name = "enabled", havingValue = "true")
public class CouponIssuedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CouponIssuedEventConsumer.class);

    @KafkaListener(topics = "${coupon.kafka.topics.coupon-issued}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(CouponIssuedEvent event) {
        log.info("coupon issued event consumed: couponIssueId={}, userId={}", event.couponIssueId(), event.userId());
    }
}
