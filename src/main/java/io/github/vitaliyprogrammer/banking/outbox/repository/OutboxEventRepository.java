package io.github.vitaliyprogrammer.banking.outbox.repository;

import io.github.vitaliyprogrammer.banking.outbox.entity.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * The next due events, locked. SKIP LOCKED lets two relay instances take two different batches at the same time,
     * instead of the second one waiting for the first, or both publishing the same events.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockDue(Instant now, int limit);

    /** For the backlog gauge; the partial index on unpublished events keeps it cheap. */
    long countByPublishedAtIsNull();

    @Query("select min(e.createdAt) from OutboxEvent e where e.publishedAt is null")
    Optional<Instant> findOldestUnpublishedCreatedAt();
}
