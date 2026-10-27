package io.github.vitaliyprogrammer.banking.account.dto.request;

import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequestDto(
        @NotNull Currency currency
) {
}
