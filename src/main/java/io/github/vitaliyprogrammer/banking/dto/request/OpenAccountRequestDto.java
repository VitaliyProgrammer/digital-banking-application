package io.github.vitaliyprogrammer.banking.dto.request;

import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequestDto(
        @NotNull
        Currency currency
) {
}
