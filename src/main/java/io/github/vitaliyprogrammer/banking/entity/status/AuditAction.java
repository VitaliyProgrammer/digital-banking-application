package io.github.vitaliyprogrammer.banking.entity.status;

/** Every action of a bank employee that the bank must be able to account for later. */
public enum AuditAction {

    CREDIT_APPLICATION_APPROVED,

    CREDIT_APPLICATION_DECLINED,

    BANK_RATE_SET,

    TICKET_TAKEN,

    TICKET_ANSWERED,

    TICKET_RESOLVED,

    /** Not a change, but access to a client's money: audited all the same. */
    CLIENT_ACCOUNTS_VIEWED
}
