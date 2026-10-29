package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.Payment;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByInitiatorIdAndIdempotencyKey(UUID initiatorId, String idempotencyKey);

    Optional<Payment> findByIdAndInitiatorId(UUID id, UUID initiatorId);

    boolean existsByQuoteId(UUID quoteId);

    /**
     * Due payments of one status, locked for the calling transaction. SKIP LOCKED: rows another worker is claiming
     * right now are skipped instead of waited for, so instances work in parallel. What stops a payment from being
     * sent twice is the lease the claim writes (next_attempt_at in the future), not SKIP LOCKED alone.
     * Native SQL because SKIP LOCKED is PostgreSQL syntax.
     */
    @Query(value = """
            SELECT * FROM payments
            WHERE status = :status AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Payment> lockDue(String status, Instant now, int limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(UUID id);
}
