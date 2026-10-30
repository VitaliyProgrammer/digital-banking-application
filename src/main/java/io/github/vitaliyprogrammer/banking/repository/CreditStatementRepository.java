package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.CreditStatement;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CreditStatementRepository extends JpaRepository<CreditStatement, UUID> {

    /**
     * Closes the month for this account. A second job run, or a second instance at the same moment, gets 0:
     * the parallel insert waits on the unique index until the first one commits.
     */
    @Modifying
    @Query(value = """
            INSERT INTO credit_statements (id, account_id, period, closing_debt, minimum_payment, closed_at,
                                           due_date, due_at, status, created_at)
            VALUES (gen_random_uuid(), :accountId, :period, :closingDebt, :minimumPayment, :closedAt,
                    :dueDate, :dueAt, 'OPEN', :createdAt)
            ON CONFLICT (account_id, period) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID accountId, String period, BigDecimal closingDebt, BigDecimal minimumPayment,
                       Instant closedAt, LocalDate dueDate, Instant dueAt, Instant createdAt);

    /**
     * Open statements past their due date, locked: a second job waits here, then finds them settled and skips them,
     * so interest is never charged twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from CreditStatement s
            where s.accountId = :accountId
              and s.status = io.github.vitaliyprogrammer.banking.entity.status.CreditStatementStatus.OPEN
              and s.dueAt <= :moment
            order by s.dueAt
            """)
    List<CreditStatement> findDueForUpdate(UUID accountId, Instant moment);

    List<CreditStatement> findAllByAccountIdOrderByClosedAtDesc(UUID accountId);
}
