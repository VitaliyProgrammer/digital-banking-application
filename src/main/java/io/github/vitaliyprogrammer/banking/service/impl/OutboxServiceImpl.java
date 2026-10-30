package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.entity.OutboxEvent;
import io.github.vitaliyprogrammer.banking.event.OutboxEventType;
import io.github.vitaliyprogrammer.banking.repository.OutboxEventRepository;
import io.github.vitaliyprogrammer.banking.service.OutboxService;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

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
}
