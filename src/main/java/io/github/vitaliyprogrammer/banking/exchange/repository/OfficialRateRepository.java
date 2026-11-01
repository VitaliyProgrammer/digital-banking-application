package io.github.vitaliyprogrammer.banking.exchange.repository;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exchange.entity.OfficialRate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OfficialRateRepository extends JpaRepository<OfficialRate, UUID> {

    Optional<OfficialRate> findFirstByCurrencyOrderByRateDateDesc(Currency currency);

    List<OfficialRate> findAllByCurrencyAndRateDateBetweenOrderByRateDate(Currency currency, LocalDate from, LocalDate to);

    /**
     * Saves the rate unless that day is already stored. Returns 1 for a new row and 0 for a known one,
     * so a repeated sync, or two instances syncing at once, changes nothing and throws nothing.
     */
    @Modifying
    @Query(value = """
            INSERT INTO official_rates (id, currency, rate_date, rate, fetched_at)
            VALUES (gen_random_uuid(), :currency, :rateDate, :rate, :fetchedAt)
            ON CONFLICT (currency, rate_date) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(String currency, LocalDate rateDate, BigDecimal rate, Instant fetchedAt);
}
