package io.github.vitaliyprogrammer.banking.entity.status;

public enum AccountType {

    /** Belongs to a client. Balance can never be negative. */
    CLIENT,

    /** A client's savings jar: earns interest, and money leaves it only back to the client's own account. */
    SAVINGS,

    /** A client's credit line, UAH only: may go below zero down to its credit limit. A negative balance is debt. */
    CREDIT,

    /** The bank's mirror of the outside world: money entering the bank is debited from it, so it may go negative. */
    SETTLEMENT,

    /** The bank's income: fees charged to clients are credited here. */
    FEE_INCOME,

    /** Money in transit: held for a payment to an external provider until the provider answers. */
    HOLD,

    /** The bank's side of currency exchanges: what it bought goes in, what it sold goes out. May go negative. */
    FX_POSITION,

    /** The bank's cost of interest paid on savings: debited on every payout, so it goes negative. */
    INTEREST_EXPENSE,

    /** The bank's marketing cost: cashback paid to clients is debited here, so it goes negative. */
    CASHBACK_EXPENSE,

    /** The bank's income from credit lines: interest charged on unpaid statements is credited here. */
    INTEREST_INCOME
}
