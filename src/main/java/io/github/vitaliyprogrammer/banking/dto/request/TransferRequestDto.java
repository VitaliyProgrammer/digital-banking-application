package io.github.vitaliyprogrammer.banking.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequestDto(
        @NotNull
        UUID fromAccountId,

        /* Recipient's IBAN; spaces are allowed, as people copy it from a banking app. */
        @NotBlank
        @Size(max = 40)
        String toAccountNumber,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount,

        @Size(max = 140)
        String description
) {
}
