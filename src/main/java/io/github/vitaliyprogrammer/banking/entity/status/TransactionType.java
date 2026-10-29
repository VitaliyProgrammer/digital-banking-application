package io.github.vitaliyprogrammer.banking.entity.status;

public enum TransactionType {

    /** Money enters the bank from outside: settlement account → client account. */
    DEPOSIT,

    /** Money moves between two accounts inside the bank. */
    TRANSFER,

    /** Client account → HOLD: money is reserved for an external payment and can no longer be spent. */
    HOLD,

    /** HOLD → settlement: the provider accepted the payment, so the money leaves the bank. */
    CAPTURE,

    /** HOLD → client account: the provider rejected the payment, so the money comes back. */
    RELEASE,

    /** Client account in one currency → FX position, FX position → client account in another: four lines. */
    EXCHANGE,

    /** Interest expense → savings account: the monthly interest payout. */
    INTEREST,

    /** Cashback expense → client account: the monthly cashback payout. */
    CASHBACK
}
