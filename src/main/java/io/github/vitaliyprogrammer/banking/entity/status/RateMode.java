package io.github.vitaliyprogrammer.banking.entity.status;

/** How the bank's buy/sell rates were set. */
public enum RateMode {

    /** A percentage around the official NBU rate; recalculated automatically when the NBU rate changes. */
    SPREAD,

    /** Exact values typed by an admin; stay fixed until an admin changes them. */
    MANUAL
}
