package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import java.util.UUID;

public interface LedgerService {

    /** Records a balanced money movement and returns the id of the ledger transaction. */
    UUID post(PostingRequestDto posting);

    /** The bank's own account of the given type (settlement, fee income) in the given currency. */
    UUID getBankAccountId(AccountType type, Currency currency);
}
