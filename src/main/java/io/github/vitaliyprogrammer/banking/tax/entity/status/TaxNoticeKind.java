package io.github.vitaliyprogrammer.banking.tax.entity.status;

public enum TaxNoticeKind {

    /** The due date is a few days away and the period is not paid yet. */
    DUE_SOON,

    /** Auto-pay is on and the day has come, but neither the tax jar nor the business account holds enough. */
    SHORTFALL
}
