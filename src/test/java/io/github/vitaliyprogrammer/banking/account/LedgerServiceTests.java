package io.github.vitaliyprogrammer.banking.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto;
import io.github.vitaliyprogrammer.banking.account.dto.request.PostingRequestDto.Line;
import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotFoundException;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import io.github.vitaliyprogrammer.banking.account.repository.AccountRepository;
import io.github.vitaliyprogrammer.banking.account.repository.LedgerTransactionRepository;
import io.github.vitaliyprogrammer.banking.account.service.impl.LedgerServiceImpl;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The ledger's own rules, with the database mocked: what a posting must look like, that it balances in every
 * currency, and that it changes either every balance or none. Locking itself is proven against PostgreSQL in
 * {@code LedgerConcurrencyIntegrationTests}; here the point is the rules, in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class LedgerServiceTests {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerTransactionRepository ledgerTransactionRepository;

    @Mock
    private EntityManager entityManager;

    private LedgerServiceImpl ledger;

    @BeforeEach
    void setUp() {
        ledger = new LedgerServiceImpl(accountRepository, ledgerTransactionRepository, entityManager,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void transferMovesMoneyAndRereadsLockedAccounts() {
        Account from = account(Currency.UAH, "500.00");
        Account to = account(Currency.UAH, "0.00");
        lockedAccounts(from, to);
        when(ledgerTransactionRepository.save(any())).thenAnswer(returnsFirstArg());

        ledger.post(PostingRequestDto.transfer(TransactionType.TRANSFER, "Rent", from.getId(), to.getId(),
                new BigDecimal("200.00")));

        assertThat(from.getBalance()).isEqualByComparingTo("300.00");
        assertThat(to.getBalance()).isEqualByComparingTo("200.00");
        // Without the refresh, a balance read earlier in the same transaction could be stale: a lost update.
        verify(entityManager).refresh(from);
        verify(entityManager).refresh(to);
        verify(ledgerTransactionRepository).save(any());
    }

    @Test
    void unbalancedPostingChangesNothing() {
        Account from = account(Currency.UAH, "500.00");
        Account to = account(Currency.UAH, "0.00");
        lockedAccounts(from, to);

        // 0.01 would appear out of nowhere.
        assertThatThrownBy(() -> ledger.post(posting(
                new Line(from.getId(), EntryDirection.DEBIT, new BigDecimal("200.00")),
                new Line(to.getId(), EntryDirection.CREDIT, new BigDecimal("200.01")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not balanced in UAH");

        assertThat(from.getBalance()).isEqualByComparingTo("500.00");
        assertThat(to.getBalance()).isZero();
        verify(ledgerTransactionRepository, never()).save(any());
    }

    @Test
    void eachCurrencyBalancesOnItsOwn() {
        Account clientUah = account(Currency.UAH, "5000.00");
        Account clientUsd = account(Currency.USD, "0.00");
        lockedAccounts(clientUah, clientUsd);

        // 4100 hryvnias out, 100 dollars in: equal in value, but each currency on its own is unbalanced.
        assertThatThrownBy(() -> ledger.post(posting(
                new Line(clientUah.getId(), EntryDirection.DEBIT, new BigDecimal("4100.00")),
                new Line(clientUsd.getId(), EntryDirection.CREDIT, new BigDecimal("100.00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not balanced");
    }

    @Test
    void failedLineRollsBackTheWholePosting() {
        Account from = account(Currency.UAH, "100.00");
        Account to = account(Currency.UAH, "0.00");
        lockedAccounts(from, to);

        assertThatThrownBy(() -> ledger.post(PostingRequestDto.transfer(TransactionType.TRANSFER, "Too much",
                from.getId(), to.getId(), new BigDecimal("150.00"))))
                .isInstanceOf(InsufficientFundsException.class);

        // In the application the exception rolls the database transaction back; nothing is saved here either.
        verify(ledgerTransactionRepository, never()).save(any());
    }

    @Test
    void unknownAccountIsNotFound() {
        Account from = account(Currency.UAH, "100.00");
        lockedAccounts(from);

        assertThatThrownBy(() -> ledger.post(PostingRequestDto.transfer(TransactionType.TRANSFER, "To nowhere",
                from.getId(), UUID.randomUUID(), BigDecimal.TEN)))
                .isInstanceOf(AccountNotFoundException.class);
    }

    static Stream<Arguments> malformedPostings() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        return Stream.of(
                Arguments.of("one line", posting(new Line(a, EntryDirection.DEBIT, BigDecimal.TEN))),
                Arguments.of("zero amount", posting(
                        new Line(a, EntryDirection.DEBIT, BigDecimal.ZERO),
                        new Line(b, EntryDirection.CREDIT, BigDecimal.ZERO))),
                Arguments.of("negative amount", posting(
                        new Line(a, EntryDirection.DEBIT, new BigDecimal("-5.00")),
                        new Line(b, EntryDirection.CREDIT, new BigDecimal("-5.00")))),
                Arguments.of("a third decimal", posting(
                        new Line(a, EntryDirection.DEBIT, new BigDecimal("10.005")),
                        new Line(b, EntryDirection.CREDIT, new BigDecimal("10.005")))),
                Arguments.of("no direction", posting(
                        new Line(a, null, BigDecimal.TEN),
                        new Line(b, EntryDirection.CREDIT, BigDecimal.TEN))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedPostings")
    void malformedPostingIsRejectedBeforeAnyLock(String what, PostingRequestDto posting) {
        assertThatThrownBy(() -> ledger.post(posting)).isInstanceOf(IllegalArgumentException.class);

        // A bug in the caller must not even lock rows.
        verifyNoInteractions(accountRepository, ledgerTransactionRepository);
    }

    @Test
    void bankAccountShardsAreReadOnce() {
        Account first = account(Currency.UAH, "0.00");
        Account second = account(Currency.UAH, "0.00");
        when(accountRepository.findAllByTypeAndCurrency(AccountType.FEE_INCOME, Currency.UAH))
                .thenReturn(List.of(first, second));

        Set<UUID> picked = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            picked.add(ledger.getBankAccountId(AccountType.FEE_INCOME, Currency.UAH));
        }

        assertThat(picked).isSubsetOf(first.getId(), second.getId());
        verify(accountRepository, times(1)).findAllByTypeAndCurrency(AccountType.FEE_INCOME, Currency.UAH);
    }

    @Test
    void clientAccountIsNotABankAccount() {
        assertThatThrownBy(() -> ledger.getBankAccountId(AccountType.CLIENT, Currency.UAH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void lockedAccounts(Account... accounts) {
        when(accountRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(accounts));
    }

    private static PostingRequestDto posting(Line... lines) {
        return new PostingRequestDto(TransactionType.TRANSFER, "Test", List.of(lines));
    }

    /** The id is generated by Hibernate on save; a unit test has no database, so it gives one itself. */
    private static Account account(Currency currency, String balance) {
        Account account = Account.openClientAccount(UUID.randomUUID(), "UA-" + currency, currency, NOW);
        ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
        BigDecimal amount = new BigDecimal(balance);
        if (amount.signum() > 0) {
            account.apply(TransactionType.DEPOSIT, EntryDirection.CREDIT, amount);
        }
        return account;
    }
}
