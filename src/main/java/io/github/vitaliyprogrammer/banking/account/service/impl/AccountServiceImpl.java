package io.github.vitaliyprogrammer.banking.account.service.impl;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.StatementEntryResponseDto;
import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.exception.AccountAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.account.repository.AccountRepository;
import io.github.vitaliyprogrammer.banking.account.repository.LedgerEntryRepository;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.common.dto.PageResponseDto;
import io.github.vitaliyprogrammer.banking.credit.exception.CreditLineNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    /** Where a card payment or a bill may take money from: the client's own money, or the bank's credit. */
    private static final List<AccountType> SPENDING_TYPES = List.of(AccountType.CLIENT, AccountType.CREDIT);

    private final AccountRepository accountRepository;

    private final LedgerEntryRepository ledgerEntryRepository;

    private final IbanGenerator ibanGenerator;

    private final Clock clock;

    @Override
    @Transactional
    public AccountResponseDto open(UUID ownerId, OpenAccountRequestDto request) {

        return openOnce(ownerId, request.currency(), AccountType.CLIENT,
                Account.openClientAccount(ownerId, ibanGenerator.generate(), request.currency(), clock.instant()));
    }

    @Override
    @Transactional
    public AccountResponseDto openSavings(UUID ownerId, Currency currency) {

        return openOnce(ownerId, currency, AccountType.SAVINGS,
                Account.openSavingsAccount(ownerId, ibanGenerator.generate(), currency, clock.instant()));
    }

    @Override
    @Transactional
    public AccountResponseDto openCreditAccount(UUID ownerId, BigDecimal creditLimit) {

        return openOnce(ownerId, Currency.UAH, AccountType.CREDIT,
                Account.openCreditAccount(ownerId, ibanGenerator.generate(), creditLimit, clock.instant()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponseDto> getAccounts(UUID ownerId) {

        return accountRepository.findAllByOwnerIdOrderByCreatedAt(ownerId).stream()
                .map(AccountResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getAccount(UUID ownerId, UUID accountId) {

        return AccountResponseDto.from(findOwnedAccount(ownerId, accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<StatementEntryResponseDto> getStatement(UUID ownerId, UUID accountId, int page, int size) {

        Account account = findOwnedAccount(ownerId, accountId);

        return PageResponseDto.from(
                ledgerEntryRepository.findStatement(account.getId(), PageRequest.of(page, size)),
                StatementEntryResponseDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountReferenceDto getOwnedAccountReference(UUID ownerId, UUID accountId) {

        return findOwnedAccount(ownerId, accountId, AccountType.CLIENT);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountReferenceDto getOwnedSpendingAccountReference(UUID ownerId, UUID accountId) {

        return accountRepository.findByIdAndOwnerIdAndTypeIn(accountId, ownerId, SPENDING_TYPES)
                .map(AccountReferenceDto::from)
                .orElseThrow(AccountNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountReferenceDto getOwnedSavingsReference(UUID ownerId, UUID accountId) {

        return findOwnedAccount(ownerId, accountId, AccountType.SAVINGS);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountReferenceDto getCreditAccountReference(UUID ownerId) {

        return AccountReferenceDto.from(findCreditAccount(ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponseDto getCreditAccount(UUID ownerId) {

        return AccountResponseDto.from(findCreditAccount(ownerId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountReferenceDto> findClientAccountByNumber(String number) {

        return accountRepository.findByNumberAndType(number, AccountType.CLIENT)
                .map(AccountReferenceDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountReferenceDto getAccountReference(UUID accountId) {

        return accountRepository.findById(accountId)
                .filter(Account::isClientOwned)
                .map(AccountReferenceDto::from)
                .orElseThrow(AccountNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountReferenceDto> getActiveSavingsAccounts() {

        return accountRepository.findAllByTypeAndStatus(AccountType.SAVINGS, AccountStatus.ACTIVE).stream()
                .map(AccountReferenceDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountReferenceDto> getActiveCreditAccounts() {

        return accountRepository.findAllByTypeAndStatus(AccountType.CREDIT, AccountStatus.ACTIVE).stream()
                .map(AccountReferenceDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalanceAt(UUID accountId, Instant moment) {

        return ledgerEntryRepository.findBalanceAt(accountId, moment).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getIncome(UUID ownerId, UUID accountId, Instant since) {

        return ledgerEntryRepository.sumIncome(accountId, ownerId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCreditedSince(UUID accountId, Instant since) {

        return ledgerEntryRepository.sumCreditedSince(accountId, since);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCreditedBetween(UUID accountId, Instant from, Instant to) {

        return ledgerEntryRepository.sumCreditedBetween(accountId, from, to);
    }

    private Account findCreditAccount(UUID ownerId) {

        return accountRepository.findByOwnerIdAndType(ownerId, AccountType.CREDIT)
                .orElseThrow(CreditLineNotFoundException::new);
    }

    /** One account of each type per currency: a current UAH account and a UAH savings jar can live side by side. */
    private AccountResponseDto openOnce(UUID ownerId, Currency currency, AccountType type, Account account) {

        if (accountRepository.existsByOwnerIdAndCurrencyAndType(ownerId, currency, type)) {
            throw new AccountAlreadyExistsException(currency);
        }

        try {
            return AccountResponseDto.from(accountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException e) {
            // Two parallel requests for the same currency: the partial unique index lets only one in.
            throw new AccountAlreadyExistsException(currency);
        }
    }

    /** Ownership is part of the query: someone else's account is simply "not found". */
    private Account findOwnedAccount(UUID ownerId, UUID accountId) {

        return accountRepository.findByIdAndOwnerId(accountId, ownerId)
                .orElseThrow(AccountNotFoundException::new);
    }

    private AccountReferenceDto findOwnedAccount(UUID ownerId, UUID accountId, AccountType type) {

        return accountRepository.findByIdAndOwnerIdAndType(accountId, ownerId, type)
                .map(AccountReferenceDto::from)
                .orElseThrow(AccountNotFoundException::new);
    }
}
