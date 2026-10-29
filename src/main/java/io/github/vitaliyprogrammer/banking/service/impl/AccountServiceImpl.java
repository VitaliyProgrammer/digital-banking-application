package io.github.vitaliyprogrammer.banking.service.impl;

import io.github.vitaliyprogrammer.banking.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.PageResponseDto;
import io.github.vitaliyprogrammer.banking.dto.response.StatementEntryResponseDto;
import io.github.vitaliyprogrammer.banking.entity.Account;
import io.github.vitaliyprogrammer.banking.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.exception.AccountAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.repository.AccountRepository;
import io.github.vitaliyprogrammer.banking.repository.LedgerEntryRepository;
import io.github.vitaliyprogrammer.banking.service.AccountService;
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
    public AccountReferenceDto getOwnedSavingsReference(UUID ownerId, UUID accountId) {

        return findOwnedAccount(ownerId, accountId, AccountType.SAVINGS);
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
    public BigDecimal getBalanceAt(UUID accountId, Instant moment) {

        return ledgerEntryRepository.findBalanceAt(accountId, moment).orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getIncome(UUID ownerId, UUID accountId, Instant since) {

        return ledgerEntryRepository.sumIncome(accountId, ownerId, since);
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
