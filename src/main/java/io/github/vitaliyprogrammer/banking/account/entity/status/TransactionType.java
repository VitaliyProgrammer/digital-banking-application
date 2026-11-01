package io.github.vitaliyprogrammer.banking.account.entity.status;

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
}
