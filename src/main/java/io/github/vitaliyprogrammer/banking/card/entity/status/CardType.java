package io.github.vitaliyprogrammer.banking.card.entity.status;

/** Each type has its own fee for card-to-card transfers (see CardTransferFeePolicy). */
public enum CardType {

    /** The bank's main card: everyday payments and payouts. A credit limit, once approved, lives on it too. */
    UNIVERSAL,

    PAYROLL,

    STUDENT,

    /** Card without plastic, for online payments. */
    VIRTUAL,

    /** Issued only after a credit decision (Credit Engine), never on a plain client request. */
    CREDIT;

    public boolean isIssuedOnRequest() {

        return this != CREDIT;
    }
}
