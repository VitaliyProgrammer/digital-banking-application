package io.github.vitaliyprogrammer.banking.account.entity.status;

import org.springframework.modulith.NamedInterface;

@NamedInterface("api")
public enum TransactionType {

    /** Money enters the bank from outside: settlement account → client account. */
    DEPOSIT,

    /** Money moves between two accounts inside the bank. */
    TRANSFER
}
