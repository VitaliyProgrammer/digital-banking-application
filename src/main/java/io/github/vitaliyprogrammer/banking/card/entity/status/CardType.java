package io.github.vitaliyprogrammer.banking.card.entity.status;

/** Each type has its own fee for card-to-card transfers (see CardTransferFeePolicy). */
public enum CardType {

    /** The bank's main card: everyday payments and payouts. A credit limit, once approved, lives on it too. */
    UNIVERSAL,

    PAYROLL,

    STUDENT,

    /** Card without plastic, for online payments. */
    VIRTUAL,

    /**
     * The earlier model: a separate card on a separate credit account. No longer issued (the limit lives on the
     * UNIVERSAL card now); kept because cards issued before still exist and the database stores the name.
     */
    CREDIT;

    public boolean isIssuedOnRequest() {

        return this != CREDIT;
    }
}
