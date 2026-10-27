package io.github.vitaliyprogrammer.banking.account.entity.status;

import org.springframework.modulith.NamedInterface;

/**
 * Seen from the account's side, like a bank statement: DEBIT takes money out, CREDIT puts money in.
 */
@NamedInterface("api")
public enum EntryDirection {
    DEBIT,
    CREDIT
}
