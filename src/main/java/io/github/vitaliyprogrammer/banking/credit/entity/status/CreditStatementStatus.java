package io.github.vitaliyprogrammer.banking.credit.entity.status;

public enum CreditStatementStatus {

    /** Closed, waiting for its due date. */
    OPEN,

    /** Repaid in full by the due date: no interest at all. */
    PAID,

    /** At least the minimum payment was made: interest on the rest, no late fee. */
    MINIMUM_PAID,

    /** Less than the minimum payment by the due date: interest on the rest and a late fee. */
    OVERDUE
}
