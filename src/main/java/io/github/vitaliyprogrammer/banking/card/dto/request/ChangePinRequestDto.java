package io.github.vitaliyprogrammer.banking.card.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChangePinRequestDto(
        /* Required when the card already has a PIN. */
        @Pattern(regexp = "\\d{4}", message = "must be 4 digits")
        String currentPin,

        @NotNull
        @Pattern(regexp = "\\d{4}", message = "must be 4 digits")
        String newPin
) {
}
