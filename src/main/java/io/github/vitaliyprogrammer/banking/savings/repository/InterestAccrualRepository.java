package io.github.vitaliyprogrammer.banking.savings.repository;

import io.github.vitaliyprogrammer.banking.savings.entity.InterestAccrual;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InterestAccrualRepository extends JpaRepository<InterestAccrual, UUID> {

    /** 1 if the day was accrued now, 0 if it already was: a repeated or parallel job run changes nothing. */
    @Modifying
    @Query(value = """
            INSERT INTO interest_accruals (id, account_id, accrual_date, balance, annual_rate, amount, created_at)
            VALUES (gen_random_uuid(), :accountId, :accrualDate, :balance, :annualRate, :amount, :createdAt)
            ON CONFLICT (account_id, accrual_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID accountId, LocalDate accrualDate, BigDecimal balance, BigDecimal annualRate,
                       BigDecimal amount, Instant createdAt);

    @Query("""
            select coalesce(sum(a.amount), 0) from InterestAccrual a
            where a.accountId = :accountId and a.accrualDate <= :until
            """)
    BigDecimal sumUntil(UUID accountId, LocalDate until);

    @Query("select coalesce(sum(a.amount), 0) from InterestAccrual a where a.accountId = :accountId")
    BigDecimal sumAll(UUID accountId);
}
