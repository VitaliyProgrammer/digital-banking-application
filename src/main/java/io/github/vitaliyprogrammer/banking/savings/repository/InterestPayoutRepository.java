package io.github.vitaliyprogrammer.banking.savings.repository;

import io.github.vitaliyprogrammer.banking.savings.entity.InterestPayout;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InterestPayoutRepository extends JpaRepository<InterestPayout, UUID> {

    /**
     * Claims the month for this account. A second job run, or a second instance at the same moment, gets 0 and
     * stops before moving any money: the parallel insert waits on the unique index until the first one commits.
     */
    @Modifying
    @Query(value = """
            INSERT INTO interest_payouts (id, account_id, period, amount, paid_at)
            VALUES (gen_random_uuid(), :accountId, :period, :amount, :paidAt)
            ON CONFLICT (account_id, period) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID accountId, String period, BigDecimal amount, Instant paidAt);

    Optional<InterestPayout> findByAccountIdAndPeriod(UUID accountId, String period);

    @Query("select coalesce(sum(p.amount), 0) from InterestPayout p where p.accountId = :accountId")
    BigDecimal sumPaid(UUID accountId);
}
