package io.github.vitaliyprogrammer.banking.account.entity.status;

public enum TransactionType {

    /** Money enters the bank from outside: settlement account → client account. */
    DEPOSIT,

    /** Money moves between two accounts inside the bank: by IBAN, into or out of a jar, a credit repayment. */
    TRANSFER,

    /** Card-to-card: like TRANSFER, but a card payment, so it may be paid with the card's credit limit. */
    CARD_TRANSFER,

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
    CASHBACK,

    /** Credit account → interest income (+ fee income for a late fee): charges for an unpaid statement. */
    CREDIT_CHARGES;

    /**
     * Spending stops at the credit limit, but the bank's charges do not: interest on a used-up credit line
     * still has to be charged, so it may take the debt past the limit.
     */
    public boolean mayExceedCreditLimit() {

        return this == CREDIT_CHARGES;
    }

    /**
     * Credit money pays for card payments and bills only. A transfer by IBAN, a jar or an exchange takes the
     * client's own money: otherwise interest-free credit could be parked in a jar to earn interest, or simply
     * taken out of the bank.
     */
    public boolean mayUseCredit() {

        return this == CARD_TRANSFER || this == HOLD || this == CREDIT_CHARGES;
    }
}
