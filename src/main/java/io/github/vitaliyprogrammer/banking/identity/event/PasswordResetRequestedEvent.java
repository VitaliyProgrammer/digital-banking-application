package io.github.vitaliyprogrammer.banking.identity.event;

/**
 * In-process only (a Spring event, never the outbox): it carries the raw token, which must not sit in a message
 * broker or a table. Sent by email after the transaction that stored the token's hash has committed.
 */
public record PasswordResetRequestedEvent(
        String email,
        String firstName,
        String rawToken
) {
}
