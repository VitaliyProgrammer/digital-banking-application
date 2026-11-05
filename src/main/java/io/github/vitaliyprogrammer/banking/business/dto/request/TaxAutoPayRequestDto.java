package io.github.vitaliyprogrammer.banking.business.dto.request;

import jakarta.validation.constraints.NotNull;

public record TaxAutoPayRequestDto(
        @NotNull
        Boolean enabled
) {
}
