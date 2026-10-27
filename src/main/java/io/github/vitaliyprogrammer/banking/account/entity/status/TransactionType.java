package io.github.vitaliyprogrammer.banking.account.entity.status;

public enum TransactionType {

    /** Money enters the bank from outside: settlement account → client account. */
    DEPOSIT,

    /** Money moves between two accounts inside the bank. */
    TRANSFER
}
