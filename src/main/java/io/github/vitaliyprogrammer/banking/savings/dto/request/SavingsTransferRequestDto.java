package io.github.vitaliyprogrammer.banking.savings.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Moving money between a savings jar and the client's own current account in the same currency. */
public record SavingsTransferRequestDto(
        /* The client's current account: the source of a deposit, the destination of a withdrawal. */
        @NotNull
        UUID accountId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount
) {
}
