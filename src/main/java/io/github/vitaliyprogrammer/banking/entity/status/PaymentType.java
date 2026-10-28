package io.github.vitaliyprogrammer.banking.entity.status;

public enum PaymentType {

    /** Money comes from outside the bank (e.g. a card of another bank) to a client account. */
    TOP_UP,

    /** Money moves from a client account to another client account in the same currency. */
    TRANSFER,

    /** Money moves from the sender's card to a recipient's card number; the fee depends on the sender's card type. */
    CARD_TRANSFER,

    /** Utilities, internet or mobile top-up: money leaves the bank to an external provider, asynchronously. */
    BILL_PAYMENT
}
