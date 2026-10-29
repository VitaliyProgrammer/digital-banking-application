package io.github.vitaliyprogrammer.banking.repository;

import io.github.vitaliyprogrammer.banking.entity.CashbackAccrual;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CashbackAccrualRepository extends JpaRepository<CashbackAccrual, UUID> {

    boolean existsByPaymentId(UUID paymentId);

    List<CashbackAccrual> findAllByUserIdAndPeriodOrderByCreatedAt(UUID userId, String period);

    @Query("""
            select coalesce(sum(a.amount), 0) from CashbackAccrual a
            where a.userId = :userId and a.period = :period
            """)
    BigDecimal sumForUser(UUID userId, String period);

    /** One row per account: what that account earned in the month, for the payout job. */
    @Query("""
            select a.accountId as accountId, sum(a.amount) as total from CashbackAccrual a
            where a.period = :period
            group by a.accountId
            """)
    List<CashbackAccountTotal> totalsByAccount(String period);

    /**
     * Makes the cap check and the insert of one client's cashback happen one at a time: two of their payments
     * completing at the same moment must not both see "200 of 500 used" and together go over the cap.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:key))) AS locked", nativeQuery = true)
    int lockForUser(String key);
}
