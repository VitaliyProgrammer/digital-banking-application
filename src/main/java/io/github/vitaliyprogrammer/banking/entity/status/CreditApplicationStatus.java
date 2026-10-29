package io.github.vitaliyprogrammer.banking.entity.status;

/** The scoring decides APPROVED or DECLINED itself, or leaves MANUAL_REVIEW for a person to decide. */
public enum CreditApplicationStatus {

    /** In the reviewers' queue: the rules were not sure. */
    MANUAL_REVIEW,

    APPROVED,

    DECLINED
}
