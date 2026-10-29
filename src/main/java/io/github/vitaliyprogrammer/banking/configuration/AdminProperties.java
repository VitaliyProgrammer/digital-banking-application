package io.github.vitaliyprogrammer.banking.configuration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from "banking.admin.*", which comes from ADMIN_EMAIL / ADMIN_PASSWORD. Both empty means no admin is created.
 * Never a migration: a password in a migration lives forever in git history and in every database copy.
 */
@Validated
@ConfigurationProperties(prefix = "banking.admin")
public record AdminProperties(
        @Email
        String email,

        /* Empty (not set) or 12 to 72 characters: BCrypt ignores everything after 72 bytes. */
        @Pattern(regexp = "|.{12,72}", message = "must be 12 to 72 characters")
        String password
) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
