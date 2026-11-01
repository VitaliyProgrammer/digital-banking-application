package io.github.vitaliyprogrammer.banking.savings.dto.response;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A savings jar. {@code accruedInterest}: earned but not paid yet; it arrives on the 1st of the next month. */
public record SavingsResponseDto(
        UUID id,
        String number,
        Currency currency,
        BigDecimal balance,
        BigDecimal annualRate,
        BigDecimal accruedInterest,
        Instant createdAt
) {
}
