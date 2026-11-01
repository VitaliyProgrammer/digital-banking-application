package io.github.vitaliyprogrammer.banking.exchange.repository;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exchange.entity.BankRate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BankRateRepository extends JpaRepository<BankRate, UUID> {

    /** Id as a tie-breaker: UUID v7 grows with time, so of two rows set in the same microsecond the later wins. */
    Optional<BankRate> findFirstByCurrencyOrderByValidFromDescIdDesc(Currency currency);

    Page<BankRate> findAllByCurrencyOrderByValidFromDescIdDesc(Currency currency, Pageable pageable);

    /**
     * Serializes changes of one currency's rate until the end of the current transaction.
     * An advisory lock, because there is no row to lock: a change appends a new row instead of updating one.
     * The function returns void, so it is wrapped in a query that returns a number.
     */
    @Query(value = "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:key))) AS locked", nativeQuery = true)
    int lockForChange(String key);
}
