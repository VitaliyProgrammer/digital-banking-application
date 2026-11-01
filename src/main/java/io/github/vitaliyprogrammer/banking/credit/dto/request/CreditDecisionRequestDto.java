package io.github.vitaliyprogrammer.banking.credit.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * A reviewer's decision. {@code approvedLimit} is required to approve and may be smaller than requested;
 * a comment is always required, so every manual decision explains itself.
 */
public record CreditDecisionRequestDto(
        @DecimalMin("500.00")
        @DecimalMax("100000.00")
        @Digits(integer = 6, fraction = 2)
        BigDecimal approvedLimit,

        @NotBlank
        @Size(max = 500)
        String comment
) {
}
