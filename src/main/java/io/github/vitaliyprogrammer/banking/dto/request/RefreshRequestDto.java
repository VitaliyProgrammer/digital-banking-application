package io.github.vitaliyprogrammer.banking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(
        @NotBlank String refreshToken
) {
}
