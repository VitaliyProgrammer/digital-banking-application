package io.github.vitaliyprogrammer.banking.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(
        @NotBlank String refreshToken
) {
}
