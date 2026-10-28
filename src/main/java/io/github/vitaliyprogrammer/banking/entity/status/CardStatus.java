package io.github.vitaliyprogrammer.banking.entity.status;

public enum CardStatus {

    ACTIVE,

    /** Blocked by the owner, e.g. a lost card. The owner can unblock it. */
    BLOCKED,

    /** Locked after too many wrong PINs. Unlocked only through support. */
    PIN_LOCKED,

    /** Final: a closed card never becomes active again. */
    CLOSED
}
