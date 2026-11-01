package io.github.vitaliyprogrammer.banking.outbox.service;

import io.github.vitaliyprogrammer.banking.outbox.event.OutboxEventType;
import java.util.UUID;

public interface OutboxService {

    /**
     * Writes an event into the caller's transaction: it is published only if that transaction commits.
     * Calling it outside of a transaction is a bug and fails at once.
     */
    void record(OutboxEventType type, UUID aggregateId, Object payload);
}
