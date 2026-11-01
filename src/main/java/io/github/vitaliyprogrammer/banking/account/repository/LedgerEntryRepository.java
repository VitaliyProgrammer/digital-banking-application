package io.github.vitaliyprogrammer.banking.account.repository;

import io.github.vitaliyprogrammer.banking.account.entity.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /** "join fetch" loads the transaction (type, description) in the same query instead of one query per row. */
    @Query(value = """
            select e from LedgerEntry e
            join fetch e.transaction
            where e.accountId = :accountId
            order by e.createdAt desc, e.id desc
            """,
            countQuery = "select count(e) from LedgerEntry e where e.accountId = :accountId")
    Page<LedgerEntry> findStatement(UUID accountId, Pageable pageable);

    /**
     * The balance at a moment in the past: the balance after the account's last entry before that moment.
     * Possible only because the ledger is append-only and every entry stores the balance after it.
     */
    @Query(value = """
            SELECT balance_after FROM ledger_entries
            WHERE account_id = :accountId AND created_at < :moment
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<BigDecimal> findBalanceAt(UUID accountId, Instant moment);

    /**
     * Money that really came to the account from outside since a moment: top-ups and transfers from OTHER people.
     * Moving one's own money (savings → current account, exchange, own card) is not income: a transaction that also
     * debited an account of the same owner is left out. Otherwise a client could raise their limit by shuffling
     * the same 1 000 ₴ back and forth.
     */
    @Query(value = """
            SELECT COALESCE(SUM(e.amount), 0) FROM ledger_entries e
            JOIN ledger_transactions t ON t.id = e.transaction_id
            WHERE e.account_id = :accountId
              AND e.direction = 'CREDIT'
              AND e.created_at >= :since
              AND t.type IN ('DEPOSIT', 'TRANSFER')
              AND NOT EXISTS (
                  SELECT 1 FROM ledger_entries other
                  JOIN accounts a ON a.id = other.account_id
                  WHERE other.transaction_id = e.transaction_id
                    AND other.direction = 'DEBIT'
                    AND a.owner_id = :ownerId)
            """, nativeQuery = true)
    BigDecimal sumIncome(UUID accountId, UUID ownerId, Instant since);

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0) FROM ledger_entries
            WHERE account_id = :accountId AND direction = 'CREDIT' AND created_at >= :since
            """, nativeQuery = true)
    BigDecimal sumCreditedSince(UUID accountId, Instant since);

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0) FROM ledger_entries
            WHERE account_id = :accountId AND direction = 'CREDIT' AND created_at >= :from AND created_at < :to
            """, nativeQuery = true)
    BigDecimal sumCreditedBetween(UUID accountId, Instant from, Instant to);
}
