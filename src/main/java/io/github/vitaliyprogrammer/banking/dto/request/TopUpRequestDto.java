package io.github.vitaliyprogrammer.banking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TopUpRequestDto(
        @NotNull
        UUID accountId,

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("100000.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount
) {
}
