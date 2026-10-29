package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.LedgerEntry;
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
}
