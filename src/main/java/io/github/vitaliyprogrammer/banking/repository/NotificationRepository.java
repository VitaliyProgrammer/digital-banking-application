package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.Notification;
import io.github.vitaliyprogrammer.banking.entity.status.EmailStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * The consumer's idempotency: the second delivery of the same event inserts nothing and returns 0.
     * Two consumers handling the same event at the same moment are serialized by the unique index.
     */
    @Modifying
    @Query(value = """
            INSERT INTO notifications (id, user_id, source_event_id, event_type, title, body, created_at,
                                       email_status, next_email_at)
            VALUES (gen_random_uuid(), :userId, :sourceEventId, :eventType, :title, :body, :createdAt,
                    :emailStatus, :nextEmailAt)
            ON CONFLICT (source_event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID userId, UUID sourceEventId, String eventType, String title, String body,
                       Instant createdAt, String emailStatus, Instant nextEmailAt);

    Page<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(UUID userId, Instant now);

    /** Emails that are due, locked; SKIP LOCKED lets two instances send two different batches. */
    @Query(value = """
            SELECT * FROM notifications
            WHERE email_status = 'PENDING' AND next_email_at <= :now
            ORDER BY next_email_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Notification> lockDueEmails(Instant now, int limit);

    long countByEmailStatus(EmailStatus emailStatus);
}
