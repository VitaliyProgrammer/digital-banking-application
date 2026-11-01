package io.github.vitaliyprogrammer.banking.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Paying the credit line back from the client's own UAH current account. */
public record CreditRepaymentRequestDto(
        @NotNull
        UUID fromAccountId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount
) {
}
