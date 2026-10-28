package io.github.vitaliyprogrammer.banking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank String email,
        @NotBlank String password
) {
}
