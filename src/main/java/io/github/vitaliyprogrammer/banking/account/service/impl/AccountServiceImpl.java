package io.github.vitaliyprogrammer.banking.account.service.impl;

import io.github.vitaliyprogrammer.banking.account.dto.request.OpenAccountRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountReferenceDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.AccountResponseDto;
import io.github.vitaliyprogrammer.banking.account.dto.response.StatementEntryResponseDto;
import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.exception.AccountAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.account.repository.AccountRepository;
import io.github.vitaliyprogrammer.banking.account.repository.LedgerEntryRepository;
import io.github.vitaliyprogrammer.banking.account.service.AccountService;
import io.github.vitaliyprogrammer.banking.shared.PageResponseDto;
import java.time.Clock;
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

        if (accountRepository.existsByOwnerIdAndCurrency(ownerId, request.currency())) {
            throw new AccountAlreadyExistsException(request.currency());
        }

        Account account = Account.openClientAccount(
                ownerId, ibanGenerator.generate(), request.currency(), clock.instant());

        try {
            return AccountResponseDto.from(accountRepository.saveAndFlush(account));
        } catch (DataIntegrityViolationException e) {
            // Two parallel requests for the same currency: the partial unique index lets only one in.
            throw new AccountAlreadyExistsException(request.currency());
        }
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

        return AccountReferenceDto.from(findOwnedAccount(ownerId, accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountReferenceDto> findClientAccountByNumber(String number) {

        return accountRepository.findByNumberAndType(number, AccountType.CLIENT)
                .map(AccountReferenceDto::from);
    }

    /** Ownership is part of the query: someone else's account is simply "not found". */
    private Account findOwnedAccount(UUID ownerId, UUID accountId) {

        return accountRepository.findByIdAndOwnerId(accountId, ownerId)
                .orElseThrow(AccountNotFoundException::new);
    }
}
