package io.github.vitaliyprogrammer.banking.payment.entity.status;

public enum PaymentType {

    /** Money comes from outside the bank (e.g. a card of another bank) to a client account. */
    TOP_UP,

    /** Money moves from a client account to another client account in the same currency. */
    TRANSFER
}
