package com.couponpublish.outbox;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class OutboxSchemaInitializer implements ApplicationRunner {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxSchemaInitializer(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        outboxEventRepository.initializeSchema();
    }
}
