package io.github.vitaliyprogrammer.banking.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequestDto(
        /* From the link in the email. */
        @NotBlank
        @Size(max = 100)
        String token,

        /* Same rules as at registration: BCrypt ignores everything after 72 bytes. */
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
