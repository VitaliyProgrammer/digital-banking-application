package io.github.vitaliyprogrammer.banking.service;

import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PageResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.StatementEntryResponseDto;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountService {

    AccountResponseDto open(UUID ownerId, OpenAccountRequestDto request);

    AccountResponseDto openSavings(UUID ownerId, Currency currency);

    List<AccountResponseDto> getAccounts(UUID ownerId);

    AccountResponseDto getAccount(UUID ownerId, UUID accountId);

    PageResponseDto<StatementEntryResponseDto> getStatement(UUID ownerId, UUID accountId, int page, int size);

    /**
     * For other modules: a CURRENT account of the given owner, or 404. Savings accounts are deliberately not found
     * here, so no transfer, card, bill payment or exchange can ever take money straight out of savings.
     */
    AccountReferenceDto getOwnedAccountReference(UUID ownerId, UUID accountId);

    /** For other modules: a savings account of the given owner, or 404. */
    AccountReferenceDto getOwnedSavingsReference(UUID ownerId, UUID accountId);

    /** For other modules: a client account by its IBAN, e.g. the recipient of a transfer. */
    Optional<AccountReferenceDto> findClientAccountByNumber(String number);

    /** For other modules: any client-owned account (current or savings) by id, e.g. for a receipt. */
    AccountReferenceDto getAccountReference(UUID accountId);

    /** For the interest job: every active savings account of every client. */
    List<AccountReferenceDto> getActiveSavingsAccounts();

    /** Balance at a past moment, read from the ledger; zero if the account had no entries before it. */
    BigDecimal getBalanceAt(UUID accountId, Instant moment);

    /** Income of a client's account since a moment: money from outside and from other people, not their own. */
    BigDecimal getIncome(UUID ownerId, UUID accountId, Instant since);
}
