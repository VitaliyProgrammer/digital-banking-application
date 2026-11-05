package io.github.vitaliyprogrammer.banking.business.entity.status;

/**
 * Single tax groups of a Ukrainian sole proprietor (ФОП). Groups 1 and 2 pay a fixed amount every month,
 * group 3 a share of every income. Only group 3 may be a VAT payer.
 */
public enum TaxGroup {

    /** Fixed monthly tax: retail at markets and household services to people. */
    GROUP_1,

    /** Fixed monthly tax: services and goods to people and to other single tax payers. */
    GROUP_2,

    /** A share of every income: 5%, or 3% for a VAT payer. */
    GROUP_3;

    public boolean paysShareOfIncome() {

        return this == GROUP_3;
    }
}
