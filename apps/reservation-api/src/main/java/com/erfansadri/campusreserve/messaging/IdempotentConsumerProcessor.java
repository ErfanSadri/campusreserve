package com.erfansadri.campusreserve.messaging;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotentConsumerProcessor {

    private final ProcessedConsumerEventRepository repository;

    public IdempotentConsumerProcessor(
            ProcessedConsumerEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean processOnce(
            String consumerName,
            UUID outboxEventId,
            Runnable sideEffect) {
        if (repository.claimIfUnprocessed(consumerName, outboxEventId) == 0) {
            return false;
        }

        sideEffect.run();
        return true;
    }
}
