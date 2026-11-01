package io.github.vitaliyprogrammer.banking.exchange.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ExchangeQuoteRequestDto(
        @NotNull
        UUID fromAccountId,

        @NotNull
        UUID toAccountId,

        /* How much to sell, in the currency of the source account. */
        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("1000000.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount
) {
}
