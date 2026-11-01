package io.github.vitaliyprogrammer.banking.credit.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreditApplicationRequestDto(
        /* The credit limit the client asks for, in UAH. */
        @NotNull
        @DecimalMin("500.00")
        @DecimalMax("100000.00")
        @Digits(integer = 6, fraction = 2)
        BigDecimal requestedLimit
) {
}
