package io.github.vitaliyprogrammer.banking.audit.entity.status;

/** What an audited action was about; together with the target id it finds the record. */
public enum AuditTarget {

    CREDIT_APPLICATION,

    /** The target id of a bank rate is the id of the rate row that was created. */
    BANK_RATE,

    SUPPORT_TICKET,

    CLIENT
}
