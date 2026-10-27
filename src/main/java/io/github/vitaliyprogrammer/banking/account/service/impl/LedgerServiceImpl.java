package io.github.vitaliyprogrammer.banking.account.service.impl;

import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.LedgerTransaction;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.account.repository.AccountRepository;
import io.github.vitaliyprogrammer.banking.account.repository.LedgerTransactionRepository;
import io.github.vitaliyprogrammer.banking.account.service.LedgerService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private static final int MAX_AMOUNT_SCALE = 2;

    private final AccountRepository accountRepository;

    private final LedgerTransactionRepository ledgerTransactionRepository;

    private final Clock clock;

    /**
     * 1. Validate the shape of the posting (programming errors → IllegalArgumentException → 500).
     * 2. Lock every involved account with one SELECT ... FOR UPDATE, ordered by id.
     * 3. Check that debits equal credits in every currency.
     * 4. Apply each line to its account (business errors: not active, insufficient funds) and record it.
     * Everything happens in one DB transaction: either all lines and balances are saved, or nothing is.
     */
    @Override
    @Transactional
    public UUID post(PostingRequestDto posting) {

        validateShape(posting);

        Set<UUID> accountIds = posting.lines().stream()
                .map(PostingRequestDto.Line::accountId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, Account> accounts = accountRepository.findAllByIdForUpdate(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));

        if (accounts.size() != accountIds.size()) {
            throw new AccountNotFoundException();
        }

        validateBalanced(posting.lines(), accounts);

        LedgerTransaction transaction = LedgerTransaction.create(
                posting.type(), posting.description(), clock.instant());

        for (PostingRequestDto.Line line : posting.lines()) {
            Account account = accounts.get(line.accountId());
            account.apply(line.direction(), line.amount());
            transaction.addEntry(account.getId(), line.direction(), line.amount(), account.getBalance());
        }

        return ledgerTransactionRepository.save(transaction).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getSettlementAccountId(Currency currency) {

        return accountRepository.findByTypeAndCurrency(AccountType.SETTLEMENT, currency)
                .map(Account::getId)
                .orElseThrow(() -> new IllegalStateException("No settlement account for " + currency));
    }

    private static void validateShape(PostingRequestDto posting) {

        if (posting.type() == null || posting.lines() == null || posting.lines().size() < 2) {
            throw new IllegalArgumentException("A posting needs a type and at least two lines");
        }

        for (PostingRequestDto.Line line : posting.lines()) {
            if (line.accountId() == null || line.direction() == null || line.amount() == null) {
                throw new IllegalArgumentException("Posting line is incomplete: " + line);
            }
            if (line.amount().signum() <= 0) {
                throw new IllegalArgumentException("Posting amount must be positive: " + line.amount());
            }
            if (line.amount().stripTrailingZeros().scale() > MAX_AMOUNT_SCALE) {
                throw new IllegalArgumentException("Posting amount has more than 2 decimal places: " + line.amount());
            }
        }
    }

    /** Double-entry rule: in every currency the total debited equals the total credited. */
    private static void validateBalanced(List<PostingRequestDto.Line> lines, Map<UUID, Account> accounts) {

        Map<Currency, BigDecimal> netByCurrency = new EnumMap<>(Currency.class);

        for (PostingRequestDto.Line line : lines) {
            BigDecimal signed = line.direction() == EntryDirection.CREDIT ? line.amount() : line.amount().negate();
            netByCurrency.merge(accounts.get(line.accountId()).getCurrency(), signed, BigDecimal::add);
        }

        netByCurrency.forEach((currency, net) -> {
            if (net.signum() != 0) {
                throw new IllegalArgumentException("Posting is not balanced in " + currency + ": " + net);
            }
        });
    }
}
