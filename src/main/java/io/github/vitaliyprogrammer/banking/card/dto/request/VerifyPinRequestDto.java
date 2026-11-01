package io.github.vitaliyprogrammer.banking.card.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyPinRequestDto(
        @NotNull
        @Pattern(regexp = "\\d{4}", message = "must be 4 digits")
        String pin
) {
}
