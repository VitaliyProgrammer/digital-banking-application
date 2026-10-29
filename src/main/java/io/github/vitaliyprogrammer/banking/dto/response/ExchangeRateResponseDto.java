package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.RateMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row of the public rates table. {@code stale = true} means today's NBU rate could not be loaded,
 * and the official rate shown is from {@code officialRateDate}. Buy/sell are null until the bank prices the currency.
 */
public record ExchangeRateResponseDto(
        Currency currency,
        BigDecimal officialRate,
        LocalDate officialRateDate,
        boolean stale,
        BigDecimal buy,
        BigDecimal sell,
        RateMode mode,
        BigDecimal spreadPercent,
        Instant updatedAt
) {
}
