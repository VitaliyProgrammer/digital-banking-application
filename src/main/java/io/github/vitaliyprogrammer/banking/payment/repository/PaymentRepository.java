package io.github.vitaliyprogrammer.banking.payment.repository;

import io.github.vitaliyprogrammer.banking.payment.entity.Payment;
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

    /**
     * Completed hryvnia payments of one client in [from, to), summed by kind: TRANSFER and CARD_TRANSFER only to
     * someone else's account, SAVINGS_DEPOSIT, and bills by their provider's category (a bill has no recipient
     * account: the money goes to the provider). Native SQL because it joins the providers and accounts tables,
     * which belong to other parts of the bank.
     */
    @Query(value = """
            SELECT CASE WHEN p.type = 'BILL_PAYMENT' THEN pr.category ELSE p.type END AS kind,
                   SUM(p.amount) AS amount
            FROM payments p
                     LEFT JOIN providers pr ON pr.id = p.provider_id
                     LEFT JOIN accounts recipient ON recipient.id = p.to_account_id
            WHERE p.initiator_id = :userId
              AND p.status = 'COMPLETED'
              AND p.currency = 'UAH'
              AND p.completed_at >= :from
              AND p.completed_at < :to
              AND (p.type IN ('SAVINGS_DEPOSIT', 'BILL_PAYMENT')
                OR (p.type IN ('TRANSFER', 'CARD_TRANSFER')
                    AND (recipient.owner_id IS NULL OR recipient.owner_id <> :userId)))
            GROUP BY kind
            """, nativeQuery = true)
    List<SpendingRow> sumSpending(UUID userId, Instant from, Instant to);
}
