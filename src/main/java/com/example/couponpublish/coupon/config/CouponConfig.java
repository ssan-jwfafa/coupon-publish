package com.example.couponpublish.coupon.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties({KafkaTopicProperties.class, FlinkProperties.class})
public class CouponConfig {

    @Bean
    NewTopic couponIssuedTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.couponIssued())
            .partitions(1)
            .replicas(1)
            .build();
    }
}
