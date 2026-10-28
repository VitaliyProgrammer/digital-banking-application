package io.github.vitaliyprogrammer.banking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank @Email @Size(max = 254) String email,
        // BCrypt silently ignores everything after 72 bytes, so longer passwords are rejected explicitly.
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName
) {
}
