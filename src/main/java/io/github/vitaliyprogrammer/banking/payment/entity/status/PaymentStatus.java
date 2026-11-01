package io.github.vitaliyprogrammer.banking.payment.entity.status;

/**
 * Synchronous payments go PENDING → COMPLETED inside one transaction.
 * Bill payments live longer: PENDING → PROCESSING → COMPLETED | FAILED, with UNKNOWN in between
 * when the provider did not answer. The allowed transitions are enforced by {@code Payment}.
 */
public enum PaymentStatus {

    /** Saved before money moves, so it reserves the idempotency key. Never visible outside its transaction. */
    PENDING,

    /** Money is held; the payment waits to be sent to the provider. */
    PROCESSING,

    /** Sent, but the provider did not answer: it may or may not have accepted. Never treated as failed. */
    UNKNOWN,

    /** The money reached its destination. */
    COMPLETED,

    /** The provider rejected the payment; the held money went back to the client. */
    FAILED
}
