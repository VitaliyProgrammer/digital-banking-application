package io.github.vitaliyprogrammer.banking.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CardTransferRequestDto(
        @NotNull
        UUID fromCardId,

        /* Recipient's 16-digit card number; spaces and dashes are allowed. */
        @NotBlank
        @Size(max = 23)
        String toCardNumber,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Size(max = 140)
        String description
) {
}
