package io.github.vitaliyprogrammer.banking.entity.status;

public enum EmailStatus {

    /** In the app only: everyday events (a top-up, a transfer) would be spam by email. */
    NOT_NEEDED,

    /** Waiting for the email job, possibly after a failed attempt. */
    PENDING,

    SENT,

    /** Gave up after the maximum number of attempts; still visible in the app. */
    FAILED
}
