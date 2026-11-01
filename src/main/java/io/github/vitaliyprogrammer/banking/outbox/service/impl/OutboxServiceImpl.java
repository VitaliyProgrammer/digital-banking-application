package io.github.vitaliyprogrammer.banking.outbox.service.impl;

import io.github.vitaliyprogrammer.banking.outbox.entity.OutboxEvent;
import io.github.vitaliyprogrammer.banking.outbox.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.outbox.repository.OutboxEventRepository;
import io.github.vitaliyprogrammer.banking.outbox.service.OutboxService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Also publishes the outbox's health as gauges. A growing backlog, or an old unpublished event, is the first sign
 * that the broker is down: it shows on the dashboard before any client notices a missing notification.
 */
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService, MeterBinder {

    private final OutboxEventRepository outboxEventRepository;

    private final JsonMapper jsonMapper;

    private final Clock clock;

    /**
     * MANDATORY, not REQUIRED: an event in a transaction of its own would commit even when the payment rolls back,
     * which is exactly the "message about money that never moved" the outbox exists to prevent.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(OutboxEventType type, UUID aggregateId, Object payload) {

        outboxEventRepository.save(OutboxEvent.create(
                type.routingKey(), aggregateId, jsonMapper.writeValueAsString(payload), clock.instant()));
    }

    /** Read on every scrape (every 15 s), not on every event: two cheap queries on a partial index. */
    @Override
    public void bindTo(MeterRegistry registry) {

        Gauge.builder("bank.outbox.unpublished", outboxEventRepository,
                        OutboxEventRepository::countByPublishedAtIsNull)
                .description("Events written but not yet confirmed by the broker")
                .register(registry);

        Gauge.builder("bank.outbox.oldest.unpublished.age", this, OutboxServiceImpl::oldestUnpublishedAgeSeconds)
                .description("How long the oldest unpublished event has been waiting")
                .baseUnit("seconds")
                .register(registry);
    }

    private double oldestUnpublishedAgeSeconds() {

        return outboxEventRepository.findOldestUnpublishedCreatedAt()
                .map(createdAt -> (double) Duration.between(createdAt, clock.instant()).toSeconds())
                .orElse(0.0);
    }
}
