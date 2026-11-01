package io.github.vitaliyprogrammer.banking.exchange.dto.request;

import io.github.vitaliyprogrammer.banking.exchange.entity.status.RateMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * SPREAD: only {@code spreadPercent}, e.g. 1.50 means buy 1.5% below and sell 1.5% above the NBU rate.
 * MANUAL: only {@code buy} and {@code sell}. Which fields go with which mode is checked by the service.
 */
public record SetBankRateRequestDto(
        @NotNull
        RateMode mode,

        @DecimalMin("0.10")
        @DecimalMax("10.00")
        @Digits(integer = 2, fraction = 2)
        BigDecimal spreadPercent,

        @DecimalMin("0.0001")
        @Digits(integer = 6, fraction = 4)
        BigDecimal buy,

        @DecimalMin("0.0001")
        @Digits(integer = 6, fraction = 4)
        BigDecimal sell,

        @Size(max = 200)
        String comment
) {
}
