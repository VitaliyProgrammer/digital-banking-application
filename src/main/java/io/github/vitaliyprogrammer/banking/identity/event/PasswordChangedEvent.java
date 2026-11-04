package io.github.vitaliyprogrammer.banking.identity.event;

/** The password was changed through a reset link: the owner is told, in case it was not them. */
public record PasswordChangedEvent(
        String email,
        String firstName
) {
}
