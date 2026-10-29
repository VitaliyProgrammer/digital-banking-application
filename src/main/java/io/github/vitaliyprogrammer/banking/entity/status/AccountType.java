package io.github.vitaliyprogrammer.banking.entity.status;

public enum AccountType {

    /** Belongs to a client. Balance can never be negative. */
    CLIENT,

    /** The bank's mirror of the outside world: money entering the bank is debited from it, so it may go negative. */
    SETTLEMENT,

    /** The bank's income: fees charged to clients are credited here. */
    FEE_INCOME,

    /** Money in transit: held for a payment to an external provider until the provider answers. */
    HOLD,

    /** The bank's side of currency exchanges: what it bought goes in, what it sold goes out. May go negative. */
    FX_POSITION
}
