package io.github.vitaliyprogrammer.banking.dto.response;

import io.github.vitaliyprogrammer.banking.entity.status.RateMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One entry of the admin's change history. {@code setByEmail} is null when the system set the rate. */
public record BankRateChangeResponseDto(
        UUID id,
        BigDecimal buy,
        BigDecimal sell,
        RateMode mode,
        BigDecimal spreadPercent,
        BigDecimal officialRate,
        UUID setBy,
        String setByEmail,
        String comment,
        Instant validFrom
) {
}
