package com.example.couponpublish.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(OrderKafkaTopicProperties.class)
public class OrderConfig {

    @Bean
    NewTopic orderEventsTopic(OrderKafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.events())
            .partitions(1)
            .replicas(1)
            .build();
    }
}
