package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.LedgerEntry;
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
}
