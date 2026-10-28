package io.github.vitaliyprogrammer.banking.entity.status;

public enum PaymentStatus {

    /** Saved before money moves, so it reserves the idempotency key. Never visible outside its transaction yet. */
    PENDING,

    /** The ledger transaction is posted. */
    COMPLETED
}
