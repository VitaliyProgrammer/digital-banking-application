package io.github.vitaliyprogrammer.banking.entity;

import io.github.vitaliyprogrammer.banking.entity.status.AccountStatus;
import io.github.vitaliyprogrammer.banking.entity.status.AccountType;
import io.github.vitaliyprogrammer.banking.entity.status.Currency;
import io.github.vitaliyprogrammer.banking.entity.status.EntryDirection;
import io.github.vitaliyprogrammer.banking.entity.status.TransactionType;
import io.github.vitaliyprogrammer.banking.exception.AccountNotActiveException;
import io.github.vitaliyprogrammer.banking.exception.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * The balance is a cached projection of the ledger: it changes only together with a ledger entry,
 * inside the same transaction and under a row lock (see {@code LedgerServiceImpl}).
 */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    private UUID ownerId;

    private String number;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance;

    /** Only on a CREDIT account: how far below zero its balance may go. */
    @Column(precision = 19, scale = 4)
    private BigDecimal creditLimit;

    private Instant createdAt;

    public static Account openClientAccount(UUID ownerId, String number, Currency currency, Instant now) {
        Account account = new Account();
        account.ownerId = ownerId;
        account.number = number;
        account.currency = currency;
        account.type = AccountType.CLIENT;
        account.status = AccountStatus.ACTIVE;
        account.balance = BigDecimal.ZERO;
        account.createdAt = now;
        return account;
    }

    public static Account openSavingsAccount(UUID ownerId, String number, Currency currency, Instant now) {
        Account account = openClientAccount(ownerId, number, currency, now);
        account.type = AccountType.SAVINGS;
        return account;
    }

    /** Credit lines are in hryvnias only: the limit comes from income in hryvnias. */
    public static Account openCreditAccount(UUID ownerId, String number, BigDecimal creditLimit, Instant now) {
        Account account = openClientAccount(ownerId, number, Currency.UAH, now);
        account.type = AccountType.CREDIT;
        account.creditLimit = creditLimit;
        return account;
    }

    /** Accounts that belong to a client: current, savings and credit. */
    public boolean isClientOwned() {
        return type == AccountType.CLIENT || type == AccountType.SAVINGS || type == AccountType.CREDIT;
    }

    /**
     * The only way to change the balance. Called by the ledger, never directly by other code, and always under the
     * row lock: two parallel payments from one credit card cannot both slip under the limit.
     * The transaction type matters only on a credit line: the bank's charges may go past its limit, spending may not.
     */
    public void apply(TransactionType transactionType, EntryDirection direction, BigDecimal amount) {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(number);
        }

        BigDecimal newBalance = direction == EntryDirection.CREDIT
                ? balance.add(amount)
                : balance.subtract(amount);

        if (direction == EntryDirection.DEBIT && isClientOwned() && !mayGoTo(newBalance, transactionType)) {
            throw new InsufficientFundsException();
        }
        balance = newBalance;
    }

    /** Zero for the client's own money; minus the limit for a credit line, or no floor for the bank's charges. */
    private boolean mayGoTo(BigDecimal newBalance, TransactionType transactionType) {
        if (type != AccountType.CREDIT) {
            return newBalance.signum() >= 0;
        }
        return transactionType.mayExceedCreditLimit() || newBalance.compareTo(creditLimit.negate()) >= 0;
    }
}
