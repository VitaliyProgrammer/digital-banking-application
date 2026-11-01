package io.github.vitaliyprogrammer.banking.cashback.repository;

import io.github.vitaliyprogrammer.banking.cashback.entity.CashbackPayout;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CashbackPayoutRepository extends JpaRepository<CashbackPayout, UUID> {

    /** Claims the month for this account; 0 means it was already paid and no money must move. */
    @Modifying
    @Query(value = """
            INSERT INTO cashback_payouts (id, account_id, period, amount, paid_at)
            VALUES (gen_random_uuid(), :accountId, :period, :amount, :paidAt)
            ON CONFLICT (account_id, period) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(UUID accountId, String period, BigDecimal amount, Instant paidAt);

    Optional<CashbackPayout> findByAccountIdAndPeriod(UUID accountId, String period);

    boolean existsByAccountIdAndPeriod(UUID accountId, String period);
}
