package io.github.vitaliyprogrammer.banking.payment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record BillPaymentRequestDto(
        @NotNull
        UUID providerId,

        /* Personal account at the provider, or a phone number for a mobile operator. */
        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "[+0-9A-Za-z-]+", message = "may contain only digits, letters, '+' and '-'")
        String accountNumber,

        @NotNull
        UUID fromAccountId,

        @NotNull
        @DecimalMin(value = "1.00", message = "must be at least 1.00")
        @DecimalMax("50000.00")
        @Digits(integer = 13, fraction = 2)
        BigDecimal amount
) {
}
