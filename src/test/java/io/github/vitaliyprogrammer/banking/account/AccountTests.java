package io.github.vitaliyprogrammer.banking.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.vitaliyprogrammer.banking.account.entity.Account;
import io.github.vitaliyprogrammer.banking.account.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.account.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.account.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.account.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.account.exception.AccountNotActiveException;
import io.github.vitaliyprogrammer.banking.account.exception.CreditLineAlreadyExistsException;
import io.github.vitaliyprogrammer.banking.account.exception.InsufficientFundsException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The rules of a balance, without a database: how far it may go below zero, and for which kind of payment.
 * {@code Account.apply()} is the only way to change a balance, so these rules hold for every module.
 */
class AccountTests {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    @Test
    void debitAndCreditMoveTheBalance() {
        Account account = uahAccount("100.00");

        account.apply(TransactionType.TRANSFER, EntryDirection.DEBIT, new BigDecimal("30.00"));
        account.apply(TransactionType.TRANSFER, EntryDirection.CREDIT, new BigDecimal("5.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("75.50");
    }

    @Test
    void ownMoneyCanBeSpentToZeroButNotBelow() {
        Account account = uahAccount("100.00");

        assertThatThrownBy(() -> account.apply(TransactionType.TRANSFER, EntryDirection.DEBIT, new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);
        // A refused payment leaves the balance as it was.
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");

        account.apply(TransactionType.TRANSFER, EntryDirection.DEBIT, new BigDecimal("100.00"));
        assertThat(account.getBalance()).isZero();
    }

    @Test
    void cardPaymentMayUseTheCreditLimitUpToItsEnd() {
        Account account = uahAccount("100.00");
        account.grantCreditLimit(new BigDecimal("1000.00"));

        // Own money first, then the whole limit.
        account.apply(TransactionType.CARD_TRANSFER, EntryDirection.DEBIT, new BigDecimal("1100.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("-1000.00");

        assertThatThrownBy(() -> account.apply(TransactionType.CARD_TRANSFER, EntryDirection.DEBIT, new BigDecimal("0.01")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transferByIbanUsesOwnMoneyOnly() {
        Account account = uahAccount("100.00");
        account.grantCreditLimit(new BigDecimal("1000.00"));

        // Otherwise interest-free credit could simply be taken out of the bank.
        assertThatThrownBy(() -> account.apply(TransactionType.TRANSFER, EntryDirection.DEBIT, new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);
        assertThatThrownBy(() -> account.apply(TransactionType.EXCHANGE, EntryDirection.DEBIT, new BigDecimal("100.01")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void bankChargesMayGoPastTheLimit() {
        Account account = uahAccount("0.00");
        account.grantCreditLimit(new BigDecimal("1000.00"));
        account.apply(TransactionType.CARD_TRANSFER, EntryDirection.DEBIT, new BigDecimal("1000.00"));

        // Interest on a used-up limit still has to be charged.
        account.apply(TransactionType.CREDIT_CHARGES, EntryDirection.DEBIT, new BigDecimal("150.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("-1150.00");
    }

    @Test
    void incomingMoneyRepaysTheDebtFirst() {
        Account account = uahAccount("0.00");
        account.grantCreditLimit(new BigDecimal("1000.00"));
        account.apply(TransactionType.CARD_TRANSFER, EntryDirection.DEBIT, new BigDecimal("500.00"));

        account.apply(TransactionType.DEPOSIT, EntryDirection.CREDIT, new BigDecimal("800.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("300.00");
    }

    @Test
    void frozenAccountTakesNoMoneyInOrOut() {
        Account account = uahAccount("100.00");
        // Nothing in the code freezes an account yet; the database row may say so, so the test sets it directly.
        ReflectionTestUtils.setField(account, "status", AccountStatus.FROZEN);

        assertThatThrownBy(() -> account.apply(TransactionType.DEPOSIT, EntryDirection.CREDIT, BigDecimal.TEN))
                .isInstanceOf(AccountNotActiveException.class);
        assertThatThrownBy(() -> account.apply(TransactionType.TRANSFER, EntryDirection.DEBIT, BigDecimal.TEN))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void creditLimitGoesOnceOnUahCurrentAccountOnly() {
        Account usd = Account.openClientAccount(UUID.randomUUID(), "UA-USD", Currency.USD, NOW);
        Account jar = Account.openSavingsAccount(UUID.randomUUID(), "UA-JAR", Currency.UAH, NOW);
        Account uah = uahAccount("0.00");

        assertThatThrownBy(() -> usd.grantCreditLimit(BigDecimal.TEN)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> jar.grantCreditLimit(BigDecimal.TEN)).isInstanceOf(IllegalStateException.class);

        uah.grantCreditLimit(new BigDecimal("5000.00"));
        assertThatThrownBy(() -> uah.grantCreditLimit(BigDecimal.TEN)).isInstanceOf(CreditLineAlreadyExistsException.class);
        assertThat(uah.getCreditLimit()).isEqualByComparingTo("5000.00");
    }

    private static Account uahAccount(String balance) {
        Account account = Account.openClientAccount(UUID.randomUUID(), "UA-UAH", Currency.UAH, NOW);
        BigDecimal amount = new BigDecimal(balance);
        if (amount.signum() > 0) {
            account.apply(TransactionType.DEPOSIT, EntryDirection.CREDIT, amount);
        }
        return account;
    }
}
