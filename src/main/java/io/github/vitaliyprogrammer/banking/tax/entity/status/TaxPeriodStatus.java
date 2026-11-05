package io.github.vitaliyprogrammer.banking.tax.entity.status;

public enum TaxPeriodStatus {

    /** Group 3: the quarter is still going on, so its tax is still growing; it may already be paid. */
    ACCRUING,

    /** Owed, the due date has not passed yet. */
    OPEN,

    /** Owed and the due date has passed. */
    OVERDUE,

    PAID,

    /** Group 3 with no income in the quarter: no tax. */
    NOTHING_DUE
}
