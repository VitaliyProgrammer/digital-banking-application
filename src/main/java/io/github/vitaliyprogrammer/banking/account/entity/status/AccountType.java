package io.github.vitaliyprogrammer.banking.account.entity.status;

public enum AccountType {

    /** Belongs to a client. Balance can never be negative. */
    CLIENT,

    /** The bank's mirror of the outside world: money entering the bank is debited from it, so it may go negative. */
    SETTLEMENT
}
